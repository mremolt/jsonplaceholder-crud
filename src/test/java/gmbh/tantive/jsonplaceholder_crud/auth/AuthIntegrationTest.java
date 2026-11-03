package gmbh.tantive.jsonplaceholder_crud.auth;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import gmbh.tantive.jsonplaceholder_crud.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@IntegrationTest
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthAccountStore accountStore;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void doesNotHideServerErrorsBehindAuthentication() throws Exception {
        mockMvc.perform(get("/error"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void documentsInheritedCrudOperationsWithConcreteResourceSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/posts/{id}'].get.responses['200'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/PostResponse"))
                .andExpect(jsonPath("$.paths['/users/{id}'].put.requestBody.content['application/json'].schema['$ref']")
                        .value("#/components/schemas/UserRequest"))
                .andExpect(jsonPath("$.paths['/users/{id}'].patch.requestBody.content['application/merge-patch+json']")
                        .exists())
                .andExpect(jsonPath("$.paths['/todos'].post.requestBody.content['application/json'].schema['$ref']")
                        .value("#/components/schemas/TodoRequest"));
    }

    @Test
    void registersLogsInAndProtectsSharedApi() throws Exception {
        String email = "auth-" + UUID.randomUUID() + "@example.com";
        String rawPassword = "Correct-horse-battery-42";
        String registration = "{\"email\":\"" + email + "\",\"password\":\"" + rawPassword + "\"}";

        mockMvc.perform(get("/posts?_start=0&_limit=5"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(registration))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        AuthAccount account = accountStore.findByEmail(email).orElseThrow();
        assertNotEquals(rawPassword, account.getPasswordHash());
        assertTrue(passwordEncoder.matches(rawPassword, account.getPasswordHash()));

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(registration))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());

        MvcResult login = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + rawPassword + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();
        JsonNode tokenResponse = objectMapper.readTree(login.getResponse().getContentAsString());
        String token = tokenResponse.get("accessToken").asString();

        mockMvc.perform(get("/posts?_start=0&_limit=5").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/posts?_start=0&_limit=5").header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsConfiguredLocalOriginsAndRejectsOtherOrigins() throws Exception {
        mockMvc.perform(options("/posts")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));

        mockMvc.perform(options("/posts")
                .header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsExpiredJwt() throws Exception {
        Instant expiredAt = Instant.now().minus(Duration.ofMinutes(2));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("jsonplaceholder-crud")
                .subject(UUID.randomUUID().toString())
                .issuedAt(expiredAt.minus(Duration.ofMinutes(15)))
                .expiresAt(expiredAt)
                .audience(List.of(JwtTokenService.AUDIENCE))
                .build();
        String expiredToken = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        mockMvc.perform(get("/posts?_start=0&_limit=5").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsJwtForAnotherAudience() throws Exception {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("jsonplaceholder-crud")
                .subject(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(5)))
                .audience(List.of("another-api"))
                .build();
        String wrongAudienceToken = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        mockMvc.perform(get("/posts?_start=0&_limit=5")
                .header("Authorization", "Bearer " + wrongAudienceToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidRegistrationInput() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rateLimitsAuthenticationAttemptsPerClientAddress() throws Exception {
        RequestPostProcessor clientAddress = request -> {
            request.setRemoteAddr("198.51.100.25");
            return request;
        };
        String invalidLogin = "{\"email\":\"missing@example.com\",\"password\":\"wrong-password\"}";

        for (int attempt = 0; attempt < AuthRateLimiter.MAX_REQUESTS_PER_WINDOW; attempt++) {
            mockMvc.perform(post("/auth/login").with(clientAddress).contentType(MediaType.APPLICATION_JSON)
                    .content(invalidLogin))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/auth/login").with(clientAddress).contentType(MediaType.APPLICATION_JSON)
                .content(invalidLogin))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"));
    }
}