package gmbh.tantive.jsonplaceholder_crud.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import gmbh.tantive.jsonplaceholder_crud.support.MutableClock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Duration REFRESH_TTL = Duration.ofDays(7);

    @Mock
    private AuthAccountStore accountStore;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenService jwtTokenService;

    private final RefreshTokenService refreshTokens = new RefreshTokenService(new InMemoryRefreshTokenStore(),
            new MutableClock(Instant.parse("2026-11-03T10:00:00Z")), REFRESH_TTL);

    @Test
    void registersNormalizedEmailWithHashedPasswordAndDefaultRole() {
        String rawPassword = "Correct-horse-battery-42";
        when(passwordEncoder.encode(rawPassword)).thenReturn("encoded-password");
        when(accountStore.insertIfAbsent(any(AuthAccount.class))).thenReturn(true);
        AuthService service = service();

        AuthAccountResponse result = service.register(new RegisterRequest("  Alice@Example.com ", rawPassword));

        assertEquals("alice@example.com", result.email());
        assertEquals("ROLE_USER", result.role());
        ArgumentCaptor<AuthAccount> account = ArgumentCaptor.forClass(AuthAccount.class);
        verify(accountStore).insertIfAbsent(account.capture());
        assertEquals("encoded-password", account.getValue().getPasswordHash());
        assertEquals("ROLE_USER", account.getValue().getRole());
    }

    @Test
    void rejectsDuplicateNormalizedEmail() {
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(accountStore.insertIfAbsent(any(AuthAccount.class))).thenReturn(false);

        assertThrows(AccountAlreadyExistsException.class,
                () -> service().register(new RegisterRequest("alice@example.com", "Correct-horse-battery-42")));
    }

    @Test
    void rejectsPasswordsLongerThanBcryptByteLimit() {
        String password = "é".repeat(40);

        assertThrows(PasswordTooLongException.class,
                () -> service().register(new RegisterRequest("alice@example.com", password)));
    }

    @Test
    void returnsAccessAndRefreshTokenAfterSuccessfulAuthentication() {
        AuthAccount account = account("alice@example.com");
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(mock(Authentication.class));
        when(accountStore.findByEmail("alice@example.com")).thenReturn(Optional.of(account));
        when(jwtTokenService.issue(account)).thenReturn(new AccessToken("signed.jwt.token", 900L));

        AuthTokenResponse result = service().login(new LoginRequest(" Alice@Example.com ", "correct-password"));

        assertEquals("signed.jwt.token", result.accessToken());
        assertEquals("Bearer", result.tokenType());
        assertEquals(900L, result.expiresIn());
        assertEquals(REFRESH_TTL.toSeconds(), result.refreshExpiresIn());
        assertEquals("alice@example.com", refreshTokens.rotate(result.refreshToken()).email());
        ArgumentCaptor<Authentication> authentication = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(authentication.capture());
        assertEquals("alice@example.com", authentication.getValue().getName());
        assertEquals("correct-password", authentication.getValue().getCredentials());
        verify(jwtTokenService).issue(account);
    }

    @Test
    void hidesWhetherLoginEmailExists() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThrows(InvalidCredentialsException.class,
                () -> service().login(new LoginRequest("missing@example.com", "wrong-password")));
    }

    @Test
    void reportsUnavailableAuthenticationInsteadOfInvalidCredentials() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new InternalAuthenticationServiceException("Cassandra unreachable"));

        assertThrows(AuthenticationUnavailableException.class,
                () -> service().login(new LoginRequest("alice@example.com", "Correct-horse-battery-42")));
    }

    @Test
    void refreshIssuesNewTokensForTheSameAccount() {
        AuthAccount account = account("alice@example.com");
        when(accountStore.findByEmail("alice@example.com")).thenReturn(Optional.of(account));
        when(jwtTokenService.issue(account)).thenReturn(new AccessToken("refreshed.jwt", 900L));
        String refreshToken = refreshTokens.issue("alice@example.com").value();

        AuthTokenResponse result = service().refresh(new RefreshTokenRequest(refreshToken));

        assertEquals("refreshed.jwt", result.accessToken());
        assertNotEquals(refreshToken, result.refreshToken());
        assertThrows(InvalidRefreshTokenException.class,
                () -> service().refresh(new RefreshTokenRequest(refreshToken)));
    }

    @Test
    void refreshFailsForDisabledOrDeletedAccounts() {
        AuthAccount disabled = new AuthAccount("alice@example.com", UUID.randomUUID(), "encoded-password",
                "ROLE_USER", false, Instant.now());
        when(accountStore.findByEmail("alice@example.com")).thenReturn(Optional.of(disabled));
        String refreshToken = refreshTokens.issue("alice@example.com").value();

        assertThrows(InvalidRefreshTokenException.class,
                () -> service().refresh(new RefreshTokenRequest(refreshToken)));
    }

    @Test
    void logoutRevokesTheRefreshToken() {
        String refreshToken = refreshTokens.issue("alice@example.com").value();

        service().logout(new RefreshTokenRequest(refreshToken));

        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokens.rotate(refreshToken));
    }

    private AuthService service() {
        return new AuthService(accountStore, passwordEncoder, authenticationManager, jwtTokenService, refreshTokens);
    }

    private AuthAccount account(String email) {
        return new AuthAccount(email, UUID.randomUUID(), "encoded-password", "ROLE_USER", true,
                Instant.now());
    }
}