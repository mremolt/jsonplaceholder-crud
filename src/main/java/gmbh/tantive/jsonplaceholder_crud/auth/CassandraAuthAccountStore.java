package gmbh.tantive.jsonplaceholder_crud.auth;

import java.util.Optional;

import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.stereotype.Repository;

@Repository
public class CassandraAuthAccountStore implements AuthAccountStore {

    private final AuthAccountRepository repository;
    private final CassandraTemplate cassandraTemplate;

    public CassandraAuthAccountStore(AuthAccountRepository repository, CassandraTemplate cassandraTemplate) {
        this.repository = repository;
        this.cassandraTemplate = cassandraTemplate;
    }

    @Override
    public Optional<AuthAccount> findByEmail(String normalizedEmail) {
        return repository.findById(normalizedEmail);
    }

    @Override
    public boolean insertIfAbsent(AuthAccount account) {
        return cassandraTemplate.insert(account, InsertOptions.builder().withIfNotExists().build()).wasApplied();
    }
}