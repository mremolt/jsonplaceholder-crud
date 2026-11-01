package gmbh.tantive.jsonplaceholder_crud.auth;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
public class AuthRateLimiter {

    static final int MAX_REQUESTS_PER_WINDOW = 10;
    private static final int MAX_CLIENTS = 10_000;
    private static final long WINDOW_NANOS = Duration.ofMinutes(1).toNanos();
    private static final long CLEANUP_INTERVAL = 128L;

    private final ConcurrentHashMap<String, ClientWindow> clients = new ConcurrentHashMap<>();
    private final AtomicLong requestCount = new AtomicLong();

    public void check(String clientAddress) {
        long now = System.nanoTime();
        if (requestCount.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            clients.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
        }
        ClientWindow window = clientWindow(clientAddress, now);
        if (!window.tryAcquire(now)) {
            throw new AuthRateLimitException(60L);
        }
    }

    private synchronized ClientWindow clientWindow(String clientAddress, long now) {
        ClientWindow existing = clients.get(clientAddress);
        if (existing != null) {
            return existing;
        }
        if (clients.size() >= MAX_CLIENTS) {
            clients.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
            if (clients.size() >= MAX_CLIENTS) {
                throw new AuthRateLimitException(60L);
            }
        }
        ClientWindow created = new ClientWindow(now);
        clients.put(clientAddress, created);
        return created;
    }

    private static final class ClientWindow {

        private long windowStartedAt;
        private long lastSeenAt;
        private int requests;

        private ClientWindow(long now) {
            windowStartedAt = now;
            lastSeenAt = now;
        }

        private synchronized boolean tryAcquire(long now) {
            if (now - windowStartedAt >= WINDOW_NANOS) {
                windowStartedAt = now;
                requests = 0;
            }
            lastSeenAt = now;
            if (requests >= MAX_REQUESTS_PER_WINDOW) {
                return false;
            }
            requests++;
            return true;
        }

        private synchronized boolean isExpired(long now) {
            return now - lastSeenAt >= WINDOW_NANOS;
        }
    }
}