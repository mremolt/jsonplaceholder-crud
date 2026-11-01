package gmbh.tantive.jsonplaceholder_crud.shared;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record CollectionPage(int start, int limit) {

    public static final int DEFAULT_LIMIT = 100;
    public static final int MAX_START = 10_000;
    public static final int MAX_LIMIT = 500;

    public CollectionPage {
        if (start < 0 || start > MAX_START) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "_start must be between 0 and 10000");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "_limit must be between 1 and 500");
        }
    }

    public int fetchLimit() {
        return start + limit;
    }
}