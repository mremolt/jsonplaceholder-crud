package gmbh.tantive.jsonplaceholder_crud.logging;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestAccessLogFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestAccessLogFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException exception) {
            int status = response.getStatus();
            logRequest(request, status >= 400 ? status : HttpServletResponse.SC_INTERNAL_SERVER_ERROR, startedAt);
            throw exception;
        }
        logRequest(request, response.getStatus(), startedAt);
    }

    private void logRequest(HttpServletRequest request, int status, long startedAt) {
        long durationMillis = (System.nanoTime() - startedAt) / 1_000_000L;
        LOGGER.info("http_request method={} path={} status={} durationMs={}", request.getMethod(),
                request.getRequestURI(), status, durationMillis);
    }
}