package gmbh.tantive.jsonplaceholder_crud.shared;

/**
 * A bounded window of a collection, either by offset ({@code start}) or by keyset cursor ({@code after}: only
 * entries with a greater id). Keyset paging costs one page per request; offset paging reads
 * {@code start + limit} entries and is therefore capped at {@link #MAX_START}.
 */
public record CollectionPage(int start, int limit, Long after) {

    public static final int DEFAULT_LIMIT = 100;
    public static final int MAX_START = 10_000;
    public static final int MAX_LIMIT = 500;

    public CollectionPage {
        if (start < 0 || start > MAX_START) {
            throw new InvalidPageException("_start must be between 0 and 10000");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new InvalidPageException("_limit must be between 1 and 500");
        }
        if (after != null && after < 0) {
            throw new InvalidPageException("_after must not be negative");
        }
        if (after != null && start != 0) {
            throw new InvalidPageException("_start and _after cannot be combined");
        }
    }

    public CollectionPage(int start, int limit) {
        this(start, limit, null);
    }

    public int fetchLimit() {
        return start + limit;
    }
}
