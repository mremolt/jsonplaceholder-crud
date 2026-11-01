package gmbh.tantive.jsonplaceholder_crud.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthAccountStore accountStore;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenService jwtTokenService;

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

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service().register(new RegisterRequest("alice@example.com", "Correct-horse-battery-42")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void rejectsPasswordsLongerThanBcryptByteLimit() {
        String password = "é".repeat(40);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service().register(new RegisterRequest("alice@example.com", password)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void returnsJwtAfterSuccessfulAuthentication() {
        AuthAccount account = account("alice@example.com");
        AuthTokenResponse token = new AuthTokenResponse("signed.jwt.token", "Bearer", 900L);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(mock(Authentication.class));
        when(accountStore.findByEmail("alice@example.com")).thenReturn(Optional.of(account));
        when(jwtTokenService.issue(account)).thenReturn(token);

        AuthTokenResponse result = service().login(new LoginRequest(" Alice@Example.com ", "correct-password"));

        assertEquals(token, result);
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

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service().login(new LoginRequest("missing@example.com", "wrong-password")));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    private AuthService service() {
        return new AuthService(accountStore, passwordEncoder, authenticationManager, jwtTokenService);
    }

    private AuthAccount account(String email) {
        return new AuthAccount(email, UUID.randomUUID(), "encoded-password", "ROLE_USER", true,
                java.time.Instant.now());
    }
}