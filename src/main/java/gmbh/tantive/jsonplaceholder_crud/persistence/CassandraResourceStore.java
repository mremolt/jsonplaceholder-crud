package gmbh.tantive.jsonplaceholder_crud.persistence;

import java.util.List;
import java.util.Optional;

import gmbh.tantive.jsonplaceholder_crud.shared.Relationship;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceRelationReference;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceStore;
import gmbh.tantive.jsonplaceholder_crud.shared.StoredResource;
import org.springframework.data.cassandra.core.CassandraBatchOperations;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CassandraResourceStore implements ResourceStore {

    // Matches CassandraRepository.save, which overwrites columns that became null.
    private static final InsertOptions INSERT_NULLS = InsertOptions.builder().withInsertNulls().build();

    private final ResourceRecordRepository repository;
    private final ResourceRelationRecordRepository relationRepository;
    private final CassandraOperations cassandraOperations;

    @Override
    public Optional<String> findPayload(String resource, long id) {
        return repository.findById(new ResourceRecordKey(resource, id)).map(ResourceRecord::getPayload);
    }

    @Override
    public List<String> findPayloads(String resource, int limit) {
        return repository.findPageByResource(resource, limit).stream().map(ResourceRecord::getPayload).toList();
    }

    @Override
    public List<String> findPayloadsAfterId(String resource, long afterId, int limit) {
        return repository.findPageAfterId(resource, afterId, limit).stream()
                .map(ResourceRecord::getPayload)
                .toList();
    }

    @Override
    public List<String> findRelatedPayloads(Relationship relationship, long parentId, String resource, int limit) {
        return relationRepository.findPage(relationship.storageName(), parentId, resource, limit).stream()
                .map(ResourceRelationRecord::getPayload)
                .toList();
    }

    @Override
    public List<String> findRelatedPayloadsAfterId(Relationship relationship, long parentId, String resource,
            long afterId, int limit) {
        return relationRepository.findPageAfterId(relationship.storageName(), parentId, resource, afterId, limit)
                .stream()
                .map(ResourceRelationRecord::getPayload)
                .toList();
    }

    @Override
    public long findHighestId(String resource) {
        return repository.findLatestByResource(resource).map(record -> record.getKey().getId()).orElse(0L);
    }

    // Logged batches keep the resources and resource_relations tables consistent after partial failures.
    @Override
    public void save(StoredResource resource, List<ResourceRelationReference> obsoleteRelations) {
        CassandraBatchOperations batch = cassandraOperations.batchOps()
                .insert(List.of(toRecord(resource)), INSERT_NULLS);
        List<ResourceRelationRecord> relations = toRelationRecords(resource);
        if (!relations.isEmpty()) {
            batch = batch.insert(relations, INSERT_NULLS);
        }
        deleteRelations(batch, resource, obsoleteRelations).execute();
    }

    @Override
    public void delete(StoredResource resource) {
        ResourceRecord record = new ResourceRecord();
        record.setKey(new ResourceRecordKey(resource.resource(), resource.id()));
        deleteRelations(cassandraOperations.batchOps().delete(record), resource, resource.relations()).execute();
    }

    @Override
    public void saveAll(List<StoredResource> resources) {
        repository.saveAll(resources.stream().map(CassandraResourceStore::toRecord).toList());
        List<ResourceRelationRecord> relations = resources.stream()
                .flatMap(resource -> toRelationRecords(resource).stream())
                .toList();
        if (!relations.isEmpty()) {
            relationRepository.saveAll(relations);
        }
    }

    private static CassandraBatchOperations deleteRelations(CassandraBatchOperations batch, StoredResource resource,
            List<ResourceRelationReference> references) {
        if (references.isEmpty()) {
            return batch;
        }
        return batch.delete(references.stream()
                .map(reference -> new ResourceRelationRecord(relationKey(resource, reference), null))
                .toList());
    }

    private static ResourceRecord toRecord(StoredResource resource) {
        return new ResourceRecord(new ResourceRecordKey(resource.resource(), resource.id()), resource.payload());
    }

    private static List<ResourceRelationRecord> toRelationRecords(StoredResource resource) {
        return resource.relations().stream()
                .map(reference -> new ResourceRelationRecord(relationKey(resource, reference), resource.payload()))
                .toList();
    }

    private static ResourceRelationKey relationKey(StoredResource resource, ResourceRelationReference reference) {
        return new ResourceRelationKey(reference.relationship().storageName(), reference.parentId(),
                resource.resource(), resource.id());
    }
}
