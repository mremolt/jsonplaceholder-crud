package gmbh.tantive.jsonplaceholder_crud.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication")
@RequiredArgsConstructor
public class AuthController {

    private static final String CLIENT_KEY = "client:";
    private static final String EMAIL_KEY = "email:";

    private final AuthService authService;
    private final AuthRateLimiter authRateLimiter;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register an account")
    public AuthAccountResponse register(@Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest) {
        checkClientLimit(servletRequest);
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive an access and a refresh token")
    public AuthTokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        checkClientLimit(servletRequest);
        authRateLimiter.check(EMAIL_KEY + AuthEmail.normalize(request.email()));
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for new tokens; the refresh token is single use")
    public AuthTokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest servletRequest) {
        checkClientLimit(servletRequest);
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "End the session of a refresh token")
    public void logout(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest servletRequest) {
        checkClientLimit(servletRequest);
        authService.logout(request);
    }

    // Behind a trusted proxy (the BFF), Tomcat resolves the browser's address from X-Forwarded-For.
    private void checkClientLimit(HttpServletRequest servletRequest) {
        authRateLimiter.check(CLIENT_KEY + servletRequest.getRemoteAddr());
    }
}