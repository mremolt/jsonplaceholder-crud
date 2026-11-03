package gmbh.tantive.jsonplaceholder_crud.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AuthRateLimiterTest {

    @Test
    void rejectsRequestsBeyondThePerClientLimit() {
        AuthRateLimiter limiter = new AuthRateLimiter();
        for (int attempt = 0; attempt < AuthRateLimiter.MAX_REQUESTS_PER_WINDOW; attempt++) {
            limiter.check("192.0.2.10");
        }

        AuthRateLimitException exception = assertThrows(AuthRateLimitException.class,
                () -> limiter.check("192.0.2.10"));

        assertEquals(60L, exception.getRetryAfterSeconds());
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