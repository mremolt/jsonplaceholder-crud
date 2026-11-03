package gmbh.tantive.jsonplaceholder_crud.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.stream.LongStream;

import gmbh.tantive.jsonplaceholder_crud.shared.Relationship;
import gmbh.tantive.jsonplaceholder_crud.shared.ResourceRelationReference;
import gmbh.tantive.jsonplaceholder_crud.shared.StoredResource;
import gmbh.tantive.jsonplaceholder_crud.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Runs {@link CassandraResourceStore} against a real Cassandra with the production schema. Every test uses
 * its own resource name, so the partitions of different tests never overlap.
 */
@IntegrationTest
class CassandraResourceStoreIntegrationTest {

    private static final ResourceRelationReference USER_7 = new ResourceRelationReference(Relationship.USER, 7L);
    private static final ResourceRelationReference USER_8 = new ResourceRelationReference(Relationship.USER, 8L);

    @Autowired
    private CassandraResourceStore store;

    private final String resource = "test-" + UUID.randomUUID();

    @Test
    void savesAndReadsAResourceWithItsRelation() {
        store.save(stored(1L, "{\"v\":1}", USER_7), List.of());

        assertThat(store.findPayload(resource, 1L)).hasValue("{\"v\":1}");
        assertThat(store.findRelatedPayloads(Relationship.USER, 7L, resource, 10)).containsExactly("{\"v\":1}");
        assertThat(store.findPayload(resource, 2L)).isEmpty();
    }

    @Test
    void movesTheRelationAndUpdatesItsPayloadInOneSave() {
        store.save(stored(1L, "{\"v\":1}", USER_7), List.of());

        store.save(stored(1L, "{\"v\":2}", USER_8), List.of(USER_7));

        assertThat(store.findRelatedPayloads(Relationship.USER, 7L, resource, 10)).isEmpty();
        assertThat(store.findRelatedPayloads(Relationship.USER, 8L, resource, 10)).containsExactly("{\"v\":2}");
        assertThat(store.findPayload(resource, 1L)).hasValue("{\"v\":2}");
    }

    @Test
    void deleteRemovesTheResourceAndAllItsRelations() {
        ResourceRelationReference completed = new ResourceRelationReference(Relationship.COMPLETED, 1L);
        store.save(stored(1L, "{}", USER_7, completed), List.of());

        store.delete(stored(1L, "{}", USER_7, completed));

        assertThat(store.findPayload(resource, 1L)).isEmpty();
        assertThat(store.findRelatedPayloads(Relationship.USER, 7L, resource, 10)).isEmpty();
        assertThat(store.findRelatedPayloads(Relationship.COMPLETED, 1L, resource, 10)).isEmpty();
    }

    @Test
    void readsBoundedPagesInIdOrder() {
        store.saveAll(LongStream.of(5, 3, 1, 4, 2).mapToObj(id -> stored(id, "{\"id\":" + id + "}", USER_7))
                .toList());

        assertThat(store.findPayloads(resource, 3)).containsExactly("{\"id\":1}", "{\"id\":2}", "{\"id\":3}");
        assertThat(store.findPayloadsAfterId(resource, 3L, 10)).containsExactly("{\"id\":4}", "{\"id\":5}");
        assertThat(store.findRelatedPayloads(Relationship.USER, 7L, resource, 2))
                .containsExactly("{\"id\":1}", "{\"id\":2}");
        assertThat(store.findHighestId(resource)).isEqualTo(5L);
    }

    @Test
    void reportsZeroAsHighestIdOfAnEmptyResource() {
        assertThat(store.findHighestId(resource)).isZero();
    }

    private StoredResource stored(long id, String payload, ResourceRelationReference... relations) {
        return new StoredResource(resource, id, payload, List.of(relations));
    }
}
