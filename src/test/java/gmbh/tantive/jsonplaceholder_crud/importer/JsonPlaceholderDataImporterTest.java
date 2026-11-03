package gmbh.tantive.jsonplaceholder_crud.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import gmbh.tantive.jsonplaceholder_crud.post.Post;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceBulkService;
import gmbh.tantive.jsonplaceholder_crud.support.RecordingBulkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class JsonPlaceholderDataImporterTest {

    @Mock
    private HttpClient httpClient;

    private final List<RecordingBulkService> resources = Stream.of("posts", "comments", "albums", "photos",
            "todos", "users").map(RecordingBulkService::new).toList();

    @Test
    void retriesServerErrorsAndImportsAllCollections() throws Exception {
        HttpResponse<String> serverError = response(503, "temporary failure");
        HttpResponse<String> success = response(200, "[]");
        Iterator<HttpResponse<String>> responses = List.of(serverError, success, success, success,
                success, success, success).iterator();
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenAnswer(invocation -> responses.next());

        importer().run(new DefaultApplicationArguments());

        ArgumentCaptor<HttpRequest> requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(7)).send(requests.capture(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        assertThat(requests.getAllValues().stream().map(HttpRequest::uri).distinct().toList()).containsExactly(
                URI.create("https://example.test/posts"), URI.create("https://example.test/comments"),
                URI.create("https://example.test/albums"), URI.create("https://example.test/photos"),
                URI.create("https://example.test/todos"), URI.create("https://example.test/users"));
        assertThat(resources).allSatisfy(resource -> assertThat(resource.imports()).containsExactly(List.of()));
    }

    @Test
    void importsTheParsedRecordsIntoTheMatchingResource() throws Exception {
        HttpResponse<String> posts = response(200, "[{\"id\":3,\"userId\":1,\"title\":\"T\",\"body\":\"B\"}]");
        HttpResponse<String> empty = response(200, "[]");
        Iterator<HttpResponse<String>> responses = List.of(posts, empty, empty, empty, empty, empty).iterator();
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenAnswer(invocation -> responses.next());

        importer().run(new DefaultApplicationArguments());

        assertThat(resources.getFirst().imports()).singleElement().satisfies(batch ->
                assertThat(batch).singleElement().satisfies(post -> {
                    assertThat(post.getId()).isEqualTo(3L);
                    assertThat(post.getTitle()).isEqualTo("T");
                }));
    }

    @Test
    void doesNotRetryClientErrors() throws Exception {
        HttpResponse<String> badRequest = response(400, "bad request");
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(badRequest);

        assertThrows(IllegalStateException.class, () -> importer().run(new DefaultApplicationArguments()));

        verify(httpClient).send(any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        assertThat(resources).allSatisfy(resource -> assertThat(resource.imports()).isEmpty());
    }

    @Test
    void stopsAfterTheMaximumNumberOfServerErrorAttempts() throws Exception {
        HttpResponse<String> serverError = response(503, "unavailable");
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenAnswer(invocation -> serverError);

        assertThrows(IllegalStateException.class, () -> importer().run(new DefaultApplicationArguments()));

        verify(httpClient, times(3)).send(any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        assertThat(resources).allSatisfy(resource -> assertThat(resource.imports()).isEmpty());
    }

    @Test
    void retriesTemporaryNetworkFailures() throws Exception {
        HttpResponse<String> success = response(200, "[]");
        AtomicInteger attempts = new AtomicInteger();
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenAnswer(invocation -> {
                    if (attempts.getAndIncrement() == 0) {
                        throw new IOException("temporary network failure");
                    }
                    return success;
                });

        importer().run(new DefaultApplicationArguments());

        verify(httpClient, times(7)).send(any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        assertThat(resources).allSatisfy(resource -> assertThat(resource.imports()).hasSize(1));
    }

    @Test
    void restoresInterruptFlagWhenRequestIsInterrupted() throws Exception {
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenThrow(new InterruptedException("cancelled"));

        try {
            assertThrows(InterruptedException.class,
                    () -> importer().run(new DefaultApplicationArguments()));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
        assertThat(resources).allSatisfy(resource -> assertThat(resource.imports()).isEmpty());
    }

    private JsonPlaceholderDataImporter importer() {
        return new JsonPlaceholderDataImporter(httpClient, new ArrayList<ResourceBulkService<?>>(resources),
                JsonMapper.builder().build(), "https://example.test/");
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> response(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        if (status >= 200 && status < 300) {
            when(response.body()).thenReturn(body);
        }
        return response;
    }
}