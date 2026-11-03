package gmbh.tantive.jsonplaceholder_crud.auth;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps the domain exceptions of the auth package to problem details. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthExceptionHandler {

    @ExceptionHandler(AuthRateLimitException.class)
    public ResponseEntity<ProblemDetail> handleRateLimit(AuthRateLimitException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.getRetryAfterSeconds()))
                .body(problem(HttpStatus.TOO_MANY_REQUESTS, exception));
    }

    @ExceptionHandler({ InvalidCredentialsException.class, InvalidRefreshTokenException.class })
    public ProblemDetail handleUnauthorized(RuntimeException exception) {
        return problem(HttpStatus.UNAUTHORIZED, exception);
    }

    @ExceptionHandler(AccountAlreadyExistsException.class)
    public ProblemDetail handleConflict(AccountAlreadyExistsException exception) {
        return problem(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler(PasswordTooLongException.class)
    public ProblemDetail handlePasswordTooLong(PasswordTooLongException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(AuthenticationUnavailableException.class)
    public ProblemDetail handleUnavailable(AuthenticationUnavailableException exception) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, exception);
    }

    private static ProblemDetail problem(HttpStatus status, RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(status, exception.getMessage());
    }
}
