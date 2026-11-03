package gmbh.tantive.jsonplaceholder_crud.support;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import gmbh.tantive.jsonplaceholder_crud.shared.Relationship;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceRelationReference;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceStore;
import gmbh.tantive.jsonplaceholder_crud.shared.StoredResource;

/**
 * In-memory implementation of {@link ResourceStore} with the same partitioning as the Cassandra tables:
 * one id-ordered partition per resource and one per (relationship, parent id, resource).
 */
public class InMemoryResourceStore implements ResourceStore {

    private final Map<String, NavigableMap<Long, String>> resources = new HashMap<>();
    private final Map<RelationPartition, NavigableMap<Long, String>> relations = new HashMap<>();

    @Override
    public Optional<String> findPayload(String resource, long id) {
        return Optional.ofNullable(partition(resource).get(id));
    }

    @Override
    public List<String> findPayloads(String resource, int limit) {
        return partition(resource).values().stream().limit(limit).toList();
    }

    @Override
    public List<String> findPayloadsAfterId(String resource, long afterId, int limit) {
        return partition(resource).tailMap(afterId, false).values().stream().limit(limit).toList();
    }

    @Override
    public List<String> findRelatedPayloads(Relationship relationship, long parentId, String resource, int limit) {
        return relationPartition(new RelationPartition(relationship, parentId, resource)).values().stream()
                .limit(limit)
                .toList();
    }

    @Override
    public long findHighestId(String resource) {
        NavigableMap<Long, String> partition = partition(resource);
        return partition.isEmpty() ? 0L : partition.lastKey();
    }

    @Override
    public void save(StoredResource resource, List<ResourceRelationReference> obsoleteRelations) {
        write(resource);
        removeRelations(resource, obsoleteRelations);
    }

    @Override
    public void delete(StoredResource resource) {
        partition(resource.resource()).remove(resource.id());
        removeRelations(resource, resource.relations());
    }

    @Override
    public void saveAll(List<StoredResource> stored) {
        stored.forEach(this::write);
    }

    private void write(StoredResource resource) {
        partition(resource.resource()).put(resource.id(), resource.payload());
        for (ResourceRelationReference reference : resource.relations()) {
            relationPartition(RelationPartition.of(resource, reference)).put(resource.id(), resource.payload());
        }
    }

    private void removeRelations(StoredResource resource, List<ResourceRelationReference> references) {
        for (ResourceRelationReference reference : references) {
            relationPartition(RelationPartition.of(resource, reference)).remove(resource.id());
        }
    }

    private NavigableMap<Long, String> partition(String resource) {
        return resources.computeIfAbsent(resource, key -> new TreeMap<>());
    }

    private NavigableMap<Long, String> relationPartition(RelationPartition partition) {
        return relations.computeIfAbsent(partition, key -> new TreeMap<>());
    }

    private record RelationPartition(Relationship relationship, long parentId, String resource) {

        static RelationPartition of(StoredResource resource, ResourceRelationReference reference) {
            return new RelationPartition(reference.relationship(), reference.parentId(), resource.resource());
        }
    }
}
