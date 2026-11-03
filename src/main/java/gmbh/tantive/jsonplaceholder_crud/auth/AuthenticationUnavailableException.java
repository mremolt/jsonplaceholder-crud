package gmbh.tantive.jsonplaceholder_crud.auth;

public class AuthenticationUnavailableException extends RuntimeException {

    public AuthenticationUnavailableException(Throwable cause) {
        super("Authentication is temporarily unavailable", cause);
    }
}
