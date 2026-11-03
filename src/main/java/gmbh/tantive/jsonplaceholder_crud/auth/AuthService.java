package gmbh.tantive.jsonplaceholder_crud.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String DEFAULT_ROLE = "ROLE_USER";
    private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;

    private final AuthAccountStore accountStore;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;

    public AuthAccountResponse register(RegisterRequest request) {
        String email = AuthEmail.normalize(request.email());
        if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
            throw new PasswordTooLongException();
        }
        AuthAccount account = new AuthAccount(email, UUID.randomUUID(), passwordEncoder.encode(request.password()),
                DEFAULT_ROLE, true, Instant.now());
        if (!accountStore.insertIfAbsent(account)) {
            throw new AccountAlreadyExistsException();
        }
        return AuthAccountResponse.from(account);
    }

    public AuthTokenResponse login(LoginRequest request) {
        String email = AuthEmail.normalize(request.email());
        if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
            throw new InvalidCredentialsException();
        }
        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
        } catch (InternalAuthenticationServiceException exception) {
            throw new AuthenticationUnavailableException(exception);
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException();
        }
        AuthAccount account = accountStore.findByEmail(email).filter(AuthAccount::isEnabled)
                .orElseThrow(InvalidCredentialsException::new);
        return AuthTokenResponse.of(jwtTokenService.issue(account), refreshTokenService.issue(account.getEmail()));
    }

    public AuthTokenResponse refresh(RefreshTokenRequest request) {
        RefreshTokenRotation rotation = refreshTokenService.rotate(request.refreshToken());
        AuthAccount account = accountStore.findByEmail(rotation.email()).filter(AuthAccount::isEnabled).orElse(null);
        if (account == null) {
            refreshTokenService.revoke(rotation.next().value());
            throw new InvalidRefreshTokenException();
        }
        return AuthTokenResponse.of(jwtTokenService.issue(account), rotation.next());
    }

    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }
}
