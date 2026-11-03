package gmbh.tantive.jsonplaceholder_crud.auth;

public class PasswordTooLongException extends RuntimeException {

    public PasswordTooLongException() {
        super("Password is too long");
    }
}
