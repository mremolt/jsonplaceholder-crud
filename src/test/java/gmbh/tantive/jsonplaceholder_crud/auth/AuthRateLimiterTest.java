package gmbh.tantive.jsonplaceholder_crud.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AuthRateLimiterTest {

    @Test
    void rejectsRequestsBeyondThePerClientLimit() {
        AuthRateLimiter limiter = new AuthRateLimiter();
        for (int attempt = 0; attempt < AuthRateLimiter.MAX_REQUESTS_PER_WINDOW; attempt++) {
            limiter.check("192.0.2.10");
        }

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> limiter.check("192.0.2.10"));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
    }

    @Test
    void tracksClientsIndependently() {
        AuthRateLimiter limiter = new AuthRateLimiter();
        for (int attempt = 0; attempt < AuthRateLimiter.MAX_REQUESTS_PER_WINDOW; attempt++) {
            limiter.check("192.0.2.10");
        }

        limiter.check("192.0.2.11");
    }
}