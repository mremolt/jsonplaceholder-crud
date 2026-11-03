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
        for (int attempt = 0; attempt < AuthRateLimiter.MAX_REQUESTS_PER_WINDOW; attempt++) {
            mockMvc.perform(post("/auth/login").with(clientAddress).contentType(MediaType.APPLICATION_JSON)
                    .content(credentials("client-limit-" + attempt + "@example.com", "wrong-password")))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/auth/login").with(clientAddress).contentType(MediaType.APPLICATION_JSON)
                .content(credentials("client-limit-last@example.com", "wrong-password")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"));
    }

    @Test
    void rateLimitsLoginAttemptsPerEmailAcrossClientAddresses() throws Exception {
        String attackedLogin = credentials("attacked-" + UUID.randomUUID() + "@example.com", "wrong-password");

        for (int attempt = 0; attempt < AuthRateLimiter.MAX_REQUESTS_PER_WINDOW; attempt++) {
            mockMvc.perform(post("/auth/login").with(fromClient("203.0.113." + attempt))
                    .contentType(MediaType.APPLICATION_JSON).content(attackedLogin))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/auth/login").with(fromClient("203.0.113.99"))
                .contentType(MediaType.APPLICATION_JSON).content(attackedLogin))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void rotatesRefreshTokensAndRevokesTheSessionOnReuse() throws Exception {
        RequestPostProcessor client = fromClient("198.51.100.40");
        String email = "refresh-" + UUID.randomUUID() + "@example.com";
        String login = credentials(email, "Correct-horse-battery-42");
        mockMvc.perform(post("/auth/register").with(client).contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isCreated());
        JsonNode first = json(mockMvc.perform(post("/auth/login").with(client)
                .contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshExpiresIn").value(Duration.ofDays(7).toSeconds()))
                .andReturn());

        JsonNode second = json(mockMvc.perform(post("/auth/refresh").with(client)
                .contentType(MediaType.APPLICATION_JSON).content(refreshBody(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn());
        mockMvc.perform(get("/posts?_limit=1").header("Authorization", "Bearer " + second.get("accessToken")
                .asString()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/refresh").with(client).contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(first)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid or expired refresh token"));
        mockMvc.perform(post("/auth/refresh").with(client).contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(second)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutEndsTheRefreshSession() throws Exception {
        RequestPostProcessor client = fromClient("198.51.100.41");
        String login = credentials("logout-" + UUID.randomUUID() + "@example.com", "Correct-horse-battery-42");
        mockMvc.perform(post("/auth/register").with(client).contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isCreated());
        JsonNode tokens = json(mockMvc.perform(post("/auth/login").with(client)
                .contentType(MediaType.APPLICATION_JSON).content(login)).andReturn());

        mockMvc.perform(post("/auth/logout").with(client).contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(tokens)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/auth/refresh").with(client).contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody(tokens)))
                .andExpect(status().isUnauthorized());
    }

    private static RequestPostProcessor fromClient(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    private static String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private static String refreshBody(JsonNode tokens) {
        return "{\"refreshToken\":\"" + tokens.get("refreshToken").asString() + "\"}";
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}