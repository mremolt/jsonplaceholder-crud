package gmbh.tantive.jsonplaceholder_crud.auth;

public class AccountAlreadyExistsException extends RuntimeException {

    public AccountAlreadyExistsException() {
        super("An account with this email already exists");
    }
}
