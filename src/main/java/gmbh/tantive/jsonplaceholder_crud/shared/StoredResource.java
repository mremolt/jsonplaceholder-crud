package gmbh.tantive.jsonplaceholder_crud.shared;

import java.util.List;

/** A serialized resource together with the relation entries that make it findable by its parents. */
public record StoredResource(String resource, long id, String payload, List<ResourceRelationReference> relations) {
}
