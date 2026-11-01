package gmbh.tantive.jsonplaceholder_crud.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;

public interface ResourceRecordRepository extends CassandraRepository<ResourceRecord, ResourceRecordKey> {

	@Query("SELECT * FROM resources WHERE resource = ?0 ORDER BY id ASC LIMIT ?1")
	List<ResourceRecord> findPageByResource(String resource, int limit);

	@Query("SELECT * FROM resources WHERE resource = ?0 AND id > ?1 LIMIT ?2")
	List<ResourceRecord> findPageAfterId(String resource, long id, int limit);

	@Query("SELECT * FROM resources WHERE resource = ?0 ORDER BY id DESC LIMIT 1")
	Optional<ResourceRecord> findLatestByResource(String resource);

}