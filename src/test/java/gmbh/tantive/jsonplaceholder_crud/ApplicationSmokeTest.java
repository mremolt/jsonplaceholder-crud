package gmbh.tantive.jsonplaceholder_crud;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import gmbh.tantive.jsonplaceholder_crud.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Drives the running application over real HTTP on its embedded server, like a client would:
 * registration, login, CRUD with relation changes and the documented error responses.
 */
@IntegrationTest
class ApplicationSmokeTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @Test
    void servesAuthenticatedCrudOverHttp() throws Exception {
        assertThat(send("GET", "/posts", null).statusCode()).isEqualTo(401);

        String credentials = """
                {"email":"smoke-%s@example.com","password":"Smoke-test-password-42"}""".formatted(UUID.randomUUID());
        assertThat(send("POST", "/auth/register", credentials).statusCode()).isEqualTo(201);
        HttpResponse<String> login = send("POST", "/auth/login", credentials);
        assertThat(login.statusCode()).isEqualTo(200);
        token = json(login).get("accessToken").asString();

        HttpResponse<String> created = send("POST", "/posts", """
                {"userId":71001,"title":"Smoke","body":"Body"}""");
        assertThat(created.statusCode()).isEqualTo(201);
        long id = json(created).get("id").asLong();
        assertThat(ids(send("GET", "/posts?userId=71001", null))).containsExactly(id);

        HttpResponse<String> patched = send("PATCH", "/posts/" + id, """
                {"title":"Patched"}""");
        assertThat(patched.statusCode()).isEqualTo(200);
        assertThat(json(patched).get("body").asString()).isEqualTo("Body");
        HttpResponse<String> invalidPatch = send("PATCH", "/posts/" + id, """
                {"title":" "}""");
        assertThat(invalidPatch.statusCode()).isEqualTo(400);
        assertThat(json(invalidPatch).get("errors").get(0).get("field").asString()).isEqualTo("title");
        assertThat(send("PATCH", "/posts/" + id, """
                {"body":null}""").statusCode()).as("null removes a required field").isEqualTo(400);

        assertThat(send("PUT", "/posts/" + id, """
                {"userId":71002,"title":"Put","body":"New"}""").statusCode()).isEqualTo(200);
        assertThat(ids(send("GET", "/posts?userId=71001", null))).isEmpty();
        assertThat(ids(send("GET", "/posts?userId=71002", null))).containsExactly(id);

        assertThat(send("DELETE", "/posts/" + id, null).statusCode()).isEqualTo(200);
        HttpResponse<String> missing = send("GET", "/posts/" + id, null);
        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(missing.headers().firstValue("Content-Type")).hasValue("application/problem+json");
        assertThat(send("GET", "/posts?_limit=0", null).statusCode()).isEqualTo(400);

        HttpResponse<String> index = send("GET", "/", null);
        assertThat(objectMapper.readValue(index.body(), String[].class))
                .containsExactly("posts", "comments", "albums", "photos", "todos", "users");
    }

    private HttpResponse<String> send(String method, String path, String body)
            throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> response) {
        return objectMapper.readTree(response.body());
    }

    private Long[] ids(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode array = json(response);
        Long[] ids = new Long[array.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = array.get(i).get("id").asLong();
        }
        return ids;
    }
}
