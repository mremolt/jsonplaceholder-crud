package gmbh.tantive.jsonplaceholder_crud.shared;

import java.util.List;
import java.util.Optional;

/**
 * Storage port for JSON resource payloads and their relation entries. All reads are bounded by a limit and
 * return payloads ordered by ascending id.
 */
public interface ResourceStore {

    Optional<String> findPayload(String resource, long id);

    List<String> findPayloads(String resource, int limit);

    List<String> findPayloadsAfterId(String resource, long afterId, int limit);

    List<String> findRelatedPayloads(Relationship relationship, long parentId, String resource, int limit);

    /** Highest stored id of the resource, or 0 if there is none. */
    long findHighestId(String resource);

    /** Writes the resource and its relations and removes the obsolete relations atomically. */
    void save(StoredResource resource, List<ResourceRelationReference> obsoleteRelations);

    /** Removes the resource and its relations atomically. */
    void delete(StoredResource resource);

    /** Bulk write for imports and backfills; not atomic across resources. */
    void saveAll(List<StoredResource> resources);
}
