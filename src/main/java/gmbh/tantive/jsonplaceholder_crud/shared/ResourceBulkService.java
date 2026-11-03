package gmbh.tantive.jsonplaceholder_crud.shared;

import java.util.List;

/**
 * Bulk access for the importer, the relation backfill and the resource index. Every resource service
 * implements it, so these components discover resources as Spring beans instead of listing them.
 */
public interface ResourceBulkService<T extends ResourceModel> {

    /** Collection name in the API and in storage, e.g. {@code posts}. */
    String resourceName();

    Class<T> resourceType();

    List<T> findAfterId(long id, int limit);

    long importAll(List<T> resources);
}
