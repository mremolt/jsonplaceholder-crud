package gmbh.tantive.jsonplaceholder_crud.logging;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestAccessLogFilterTest {

    @Test
    void logsRequestMetadataWithoutHeadersBodyOrQueryParameters() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestAccessLogFilter.class);
        Level originalLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.INFO);
        logger.addAppender(appender);

        try {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
            request.setQueryString("email=private@example.com&password=query-secret");
            request.addHeader("Authorization", "Bearer header-secret");
            request.setContent("{\"password\":\"body-secret\"}".getBytes(StandardCharsets.UTF_8));
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = (servletRequest, servletResponse) -> ((HttpServletResponse) servletResponse)
                    .setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            new RequestAccessLogFilter().doFilter(request, response, chain);
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(originalLevel);
            appender.stop();
        }

        assertEquals(1, appender.list.size());
        String logMessage = appender.list.getFirst().getFormattedMessage();
        assertTrue(logMessage.contains("method=POST"));
        assertTrue(logMessage.contains("path=/auth/login"));
        assertTrue(logMessage.contains("status=401"));
        assertFalse(logMessage.contains("private@example.com"));
        assertFalse(logMessage.contains("query-secret"));
        assertFalse(logMessage.contains("header-secret"));
        assertFalse(logMessage.contains("body-secret"));
    }
}