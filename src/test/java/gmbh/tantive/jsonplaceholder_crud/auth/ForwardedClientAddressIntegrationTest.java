package gmbh.tantive.jsonplaceholder_crud.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import gmbh.tantive.jsonplaceholder_crud.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Runs over real HTTP because the client address comes from Tomcat's RemoteIpValve: a BFF on a trusted proxy
 * address (loopback by default) passes the browser's address in X-Forwarded-For, and rate limits apply per
 * browser instead of per BFF.
 */
@IntegrationTest
class ForwardedClientAddressIntegrationTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Test
    void rateLimitsTheForwardedClientInsteadOfTheTrustedProxy() throws Exception {
        for (int attempt = 0; attempt < AuthRateLimiter.MAX_REQUESTS_PER_WINDOW; attempt++) {
            assertThat(failedLogin("192.0.2.77")).isEqualTo(401);
        }

        assertThat(failedLogin("192.0.2.77")).isEqualTo(429);
        assertThat(failedLogin("192.0.2.78")).as("another browser behind the same BFF").isEqualTo(401);
    }

    private int failedLogin(String forwardedFor) throws Exception {
        String body = "{\"email\":\"forwarded-" + UUID.randomUUID() + "@example.com\",\"password\":\"wrong-password\"}";
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/auth/login"))
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", forwardedFor)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
