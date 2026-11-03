package gmbh.tantive.jsonplaceholder_crud.shared;

/**
 * Every relation partition a resource can be listed by. The storage names are persisted in
 * {@code resource_relations.relationship}; changing one requires a data migration.
 */
public enum Relationship {

    USER("user"),
    POST("post"),
    ALBUM("album"),
    /** Parent id 1 lists completed todos, 0 open ones. */
    COMPLETED("completed"),
    USER_COMPLETED("user-completed-true"),
    USER_OPEN("user-completed-false");

    private final String storageName;

    Relationship(String storageName) {
        this.storageName = storageName;
    }

    public String storageName() {
        return storageName;
    }

    public static Relationship userCompletion(boolean completed) {
        return completed ? USER_COMPLETED : USER_OPEN;
    }
}
