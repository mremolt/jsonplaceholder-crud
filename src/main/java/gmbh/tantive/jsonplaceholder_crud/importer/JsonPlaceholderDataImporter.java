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
import gmbh.tantive.jsonplaceholder_crud.album.Album;
import gmbh.tantive.jsonplaceholder_crud.album.AlbumService;
import gmbh.tantive.jsonplaceholder_crud.comment.Comment;
import gmbh.tantive.jsonplaceholder_crud.comment.CommentService;
import gmbh.tantive.jsonplaceholder_crud.photo.Photo;
import gmbh.tantive.jsonplaceholder_crud.photo.PhotoService;
import gmbh.tantive.jsonplaceholder_crud.post.Post;
import gmbh.tantive.jsonplaceholder_crud.post.PostService;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceCrudService;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceModel;
import gmbh.tantive.jsonplaceholder_crud.todo.Todo;
import gmbh.tantive.jsonplaceholder_crud.todo.TodoService;
import gmbh.tantive.jsonplaceholder_crud.user.User;
import gmbh.tantive.jsonplaceholder_crud.user.UserService;

@Component
@ConditionalOnProperty(name = "jsonplaceholder.import.enabled", havingValue = "true")
public class JsonPlaceholderDataImporter implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonPlaceholderDataImporter.class);
    private static final int MAX_ATTEMPTS = 3;

    private final HttpClient httpClient;
    private final PostService postService;
    private final CommentService commentService;
    private final AlbumService albumService;
    private final PhotoService photoService;
    private final TodoService todoService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public JsonPlaceholderDataImporter(HttpClient httpClient, PostService postService, CommentService commentService,
            AlbumService albumService, PhotoService photoService, TodoService todoService, UserService userService,
            ObjectMapper objectMapper, @Value("${jsonplaceholder.import.base-url}") String baseUrl) {
        this.httpClient = httpClient;
        this.postService = postService;
        this.commentService = commentService;
        this.albumService = albumService;
        this.photoService = photoService;
        this.todoService = todoService;
        this.userService = userService;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        importResource("posts", Post.class, postService);
        importResource("comments", Comment.class, commentService);
        importResource("albums", Album.class, albumService);
        importResource("photos", Photo.class, photoService);
        importResource("todos", Todo.class, todoService);
        importResource("users", User.class, userService);
    }

    private <T extends ResourceModel> void importResource(String name, Class<T> type,
            ResourceCrudService<T> service) throws Exception {
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
            records.add(objectMapper.treeToValue(recordNode, type));
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