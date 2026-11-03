package gmbh.tantive.jsonplaceholder_crud.persistence;

import java.util.List;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;

public interface ResourceRelationRecordRepository
        extends CassandraRepository<ResourceRelationRecord, ResourceRelationKey> {

    @Query("SELECT * FROM resource_relations WHERE relationship = ?0 AND parent_id = ?1 AND resource = ?2 LIMIT ?3")
    List<ResourceRelationRecord> findPage(String relationship, Long parentId, String resource, int limit);

    @Query("SELECT * FROM resource_relations WHERE relationship = ?0 AND parent_id = ?1 AND resource = ?2"
            + " AND id > ?3 LIMIT ?4")
    List<ResourceRelationRecord> findPageAfterId(String relationship, Long parentId, String resource, long afterId,
            int limit);
}