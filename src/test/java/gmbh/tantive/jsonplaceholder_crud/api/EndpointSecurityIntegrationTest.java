package gmbh.tantive.jsonplaceholder_crud.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import gmbh.tantive.jsonplaceholder_crud.support.IntegrationTest;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Discovers every MVC endpoint of the running application and checks that all of them except an explicit
 * allowlist reject requests without a bearer token. New endpoints are covered automatically.
 */
@IntegrationTest
class EndpointSecurityIntegrationTest {

    private static final Set<String> PUBLIC_ENDPOINTS = Set.of("POST /auth/register", "POST /auth/login",
            "POST /auth/refresh", "POST /auth/logout");
    private static final List<String> PUBLIC_PATH_PREFIXES = List.of("/error", "/v3/api-docs", "/swagger-ui");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @TestFactory
    Stream<DynamicTest> rejectsEveryNonPublicEndpointWithoutToken() {
        List<Endpoint> protectedEndpoints = endpoints().stream()
                .filter(endpoint -> !endpoint.isPublic())
                .toList();

        assertThat(protectedEndpoints).extracting(Endpoint::toString)
                .contains("GET /", "GET /posts", "GET /posts/{id:\\d+}", "PATCH /users/{id:\\d+}",
                        "DELETE /todos/{id:\\d+}", "GET /albums/{id:\\d+}/photos");
        return protectedEndpoints.stream().map(endpoint -> dynamicTest(endpoint.toString(),
                () -> mockMvc.perform(request(endpoint.method(), endpoint.samplePath()))
                        .andExpect(status().isUnauthorized())));
    }

    private List<Endpoint> endpoints() {
        return handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(EndpointSecurityIntegrationTest::endpoints)
                .distinct()
                .sorted(Comparator.comparing(Endpoint::toString))
                .toList();
    }

    private static Stream<Endpoint> endpoints(RequestMappingInfo info) {
        Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
        Stream<HttpMethod> httpMethods = methods.isEmpty() ? Stream.of(HttpMethod.GET)
                : methods.stream().map(RequestMethod::asHttpMethod);
        List<HttpMethod> resolvedMethods = httpMethods.toList();
        return info.getPathPatternsCondition().getPatternValues().stream()
                .flatMap(pattern -> resolvedMethods.stream().map(method -> new Endpoint(method, pattern)));
    }

    private record Endpoint(HttpMethod method, String pattern) {

        boolean isPublic() {
            return PUBLIC_ENDPOINTS.contains(toString())
                    || PUBLIC_PATH_PREFIXES.stream().anyMatch(pattern::startsWith);
        }

        String samplePath() {
            return pattern.replaceAll("\\{[^/]+}", "1");
        }

        @Override
        public String toString() {
            return method.name() + " " + pattern;
        }
    }
}
