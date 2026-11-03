package gmbh.tantive.jsonplaceholder_crud.shared;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, long id) {
        super("No " + resourceName + " entry with id " + id);
    }
}
