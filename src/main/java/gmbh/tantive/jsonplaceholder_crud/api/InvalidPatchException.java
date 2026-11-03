package gmbh.tantive.jsonplaceholder_crud.api;

public class InvalidPatchException extends RuntimeException {

    public InvalidPatchException(String message) {
        super(message);
    }
}
