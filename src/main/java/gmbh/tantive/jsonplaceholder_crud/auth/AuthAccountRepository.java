package gmbh.tantive.jsonplaceholder_crud.auth;

import org.springframework.data.cassandra.repository.CassandraRepository;

public interface AuthAccountRepository extends CassandraRepository<AuthAccount, String> {
}