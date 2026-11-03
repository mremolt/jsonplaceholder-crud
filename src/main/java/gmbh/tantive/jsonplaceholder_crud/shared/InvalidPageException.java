package gmbh.tantive.jsonplaceholder_crud.shared;

public class InvalidPageException extends RuntimeException {

    public InvalidPageException(String message) {
        super(message);
    }
}
