package gmbh.tantive.jsonplaceholder_crud.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import gmbh.tantive.jsonplaceholder_crud.api.JsonMergePatcher;
import gmbh.tantive.jsonplaceholder_crud.auth.AuthRateLimiter;
import gmbh.tantive.jsonplaceholder_crud.auth.AuthSecurityConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Web layer test with the production security filter chain, Jackson setup and exception handlers. Requests
 * carry a valid JWT by default; unauthenticated access is covered by {@code EndpointSecurityIntegrationTest}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@WebMvcTest
@Import({ AuthSecurityConfiguration.class, JsonMergePatcher.class, ApiWebMvcTest.AuthenticatedRequests.class })
@MockitoBean(types = { UserDetailsService.class, AuthRateLimiter.class })
@TestPropertySource(properties = "security.jwt.secret=" + IntegrationTest.JWT_SECRET)
public @interface ApiWebMvcTest {

    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] value();

    @TestConfiguration(proxyBeanMethods = false)
    class AuthenticatedRequests {

        @Bean
        MockMvcBuilderCustomizer authenticatedByDefault() {
            return builder -> builder.defaultRequest(get("/").with(jwt()));
        }
    }
}
