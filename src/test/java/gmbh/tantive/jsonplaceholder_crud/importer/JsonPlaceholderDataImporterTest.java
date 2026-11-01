package gmbh.tantive.jsonplaceholder_crud.importer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import gmbh.tantive.jsonplaceholder_crud.album.AlbumService;
import gmbh.tantive.jsonplaceholder_crud.comment.CommentService;
import gmbh.tantive.jsonplaceholder_crud.photo.PhotoService;
import gmbh.tantive.jsonplaceholder_crud.post.PostService;
import gmbh.tantive.jsonplaceholder_crud.todo.TodoService;
import gmbh.tantive.jsonplaceholder_crud.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class JsonPlaceholderDataImporterTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private PostService postService;

    @Mock
    private CommentService commentService;

    @Mock
    private AlbumService albumService;

    @Mock
    private PhotoService photoService;

    @Mock
    private TodoService todoService;

    @Mock
    private UserService userService;

    @Test
    void retriesServerErrorsAndImportsAllCollections() throws Exception {
        HttpResponse<String> serverError = response(503, "temporary failure");
        HttpResponse<String> success = response(200, "[]");
        Iterator<HttpResponse<String>> responses = List.of(serverError, success, success, success,
                success, success, success).iterator();
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenAnswer(invocation -> responses.next());

        importer().run(new DefaultApplicationArguments());

        verify(httpClient, times(7)).send(any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        verify(postService).importAll(List.of());
        verify(commentService).importAll(List.of());
        verify(albumService).importAll(List.of());
        verify(photoService).importAll(List.of());
        verify(todoService).importAll(List.of());
        verify(userService).importAll(List.of());
    }

    @Test
    void doesNotRetryClientErrors() throws Exception {
        HttpResponse<String> badRequest = response(400, "bad request");
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(badRequest);

        assertThrows(IllegalStateException.class, () -> importer().run(new DefaultApplicationArguments()));

        verify(httpClient).send(any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        verifyNoInteractions(postService, commentService, albumService, photoService, todoService, userService);
    }

    @Test
    void stopsAfterTheMaximumNumberOfServerErrorAttempts() throws Exception {
        HttpResponse<String> serverError = response(503, "unavailable");
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenAnswer(invocation -> serverError);

        assertThrows(IllegalStateException.class, () -> importer().run(new DefaultApplicationArguments()));

        verify(httpClient, times(3)).send(any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        verifyNoInteractions(postService, commentService, albumService, photoService, todoService, userService);
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
        verifyNoInteractions(postService, commentService, albumService, photoService, todoService, userService);
    }

    private JsonPlaceholderDataImporter importer() {
        return new JsonPlaceholderDataImporter(httpClient, postService, commentService, albumService, photoService,
                todoService, userService, JsonMapper.builder().build(), "https://example.test/");
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