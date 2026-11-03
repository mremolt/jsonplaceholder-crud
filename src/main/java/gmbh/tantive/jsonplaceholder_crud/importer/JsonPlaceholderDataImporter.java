package gmbh.tantive.jsonplaceholder_crud.importer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceBulkService;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceModel;

@Component
@ConditionalOnProperty(name = "jsonplaceholder.import.enabled", havingValue = "true")
public class JsonPlaceholderDataImporter implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonPlaceholderDataImporter.class);
    private static final int MAX_ATTEMPTS = 3;

    private final HttpClient httpClient;
    private final List<ResourceBulkService<?>> resources;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public JsonPlaceholderDataImporter(HttpClient httpClient, List<ResourceBulkService<?>> resources,
            ObjectMapper objectMapper, @Value("${jsonplaceholder.import.base-url}") String baseUrl) {
        this.httpClient = httpClient;
        this.resources = resources;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        for (ResourceBulkService<?> resource : resources) {
            importResource(resource);
        }
    }

    private <T extends ResourceModel> void importResource(ResourceBulkService<T> service) throws Exception {
        String name = service.resourceName();
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/" + name))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> response = sendWithRetry(request, name);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("JSONPlaceholder returned HTTP " + response.statusCode()
                    + " for " + name);
        }
        JsonNode recordsNode = objectMapper.readTree(response.body());
        List<T> records = new ArrayList<>(recordsNode.size());
        for (JsonNode recordNode : recordsNode) {
            records.add(objectMapper.treeToValue(recordNode, service.resourceType()));
        }
        long imported = service.importAll(records);
        LOGGER.info("Imported {} {} records from JSONPlaceholder", imported, name);
    }

    private HttpResponse<String> sendWithRetry(HttpRequest request, String resource)
            throws IOException, InterruptedException {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw exception;
            } catch (IOException exception) {
                if (attempt == MAX_ATTEMPTS) {
                    throw exception;
                }
                LOGGER.warn("JSONPlaceholder request failed for {}; retry {}/{}", resource, attempt, MAX_ATTEMPTS,
                        exception);
                waitBeforeRetry(attempt);
                continue;
            }

            int status = response.statusCode();
            if ((status == 429 || status >= 500) && attempt < MAX_ATTEMPTS) {
                LOGGER.warn("JSONPlaceholder returned HTTP {} for {}; retry {}/{}", status, resource, attempt,
                        MAX_ATTEMPTS);
                waitBeforeRetry(attempt);
                continue;
            }
            return response;
        }
        throw new IllegalStateException("No response received from JSONPlaceholder for " + resource);
    }

    private void waitBeforeRetry(int attempt) throws InterruptedException {
        try {
            Thread.sleep(200L << (attempt - 1));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        }
    }
}