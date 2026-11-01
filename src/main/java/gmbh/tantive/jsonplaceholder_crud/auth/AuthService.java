package gmbh.tantive.jsonplaceholder_crud.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final String DEFAULT_ROLE = "ROLE_USER";
    private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;

    private final AuthAccountStore accountStore;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    public AuthService(AuthAccountStore accountStore, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, JwtTokenService jwtTokenService) {
        this.accountStore = accountStore;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
    }

    public AuthAccountResponse register(RegisterRequest request) {
        String email = AuthEmail.normalize(request.email());
        if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is too long");
        }
        AuthAccount account = new AuthAccount(email, UUID.randomUUID(), passwordEncoder.encode(request.password()),
                DEFAULT_ROLE, true, Instant.now());
        if (!accountStore.insertIfAbsent(account)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        return AuthAccountResponse.from(account);
    }

    public AuthTokenResponse login(LoginRequest request) {
        String email = AuthEmail.normalize(request.email());
        if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
            throw invalidCredentials();
        }
        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
        } catch (AuthenticationException exception) {
            throw invalidCredentials();
        }
        AuthAccount account = accountStore.findByEmail(email).filter(candidate -> candidate.isEnabled())
                .orElseThrow(AuthService::invalidCredentials);
        return jwtTokenService.issue(account);
    }

    private static ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
}