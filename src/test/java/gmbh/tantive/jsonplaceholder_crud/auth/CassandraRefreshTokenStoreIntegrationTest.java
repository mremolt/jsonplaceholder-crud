package gmbh.tantive.jsonplaceholder_crud.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import gmbh.tantive.jsonplaceholder_crud.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.core.CassandraOperations;

@IntegrationTest
class CassandraRefreshTokenStoreIntegrationTest {

    @Autowired
    private CassandraRefreshTokenStore store;

    @Autowired
    private CassandraOperations cassandraOperations;

    private final String hash = "hash-" + UUID.randomUUID();
    private final UUID family = UUID.randomUUID();
    private final Instant expiresAt = Instant.now().plus(Duration.ofHours(1)).truncatedTo(ChronoUnit.MILLIS);

    @Test
    void storesTokensWithAnExpiringTtl() {
        store.save(new StoredRefreshToken(hash, family, "alice@example.com", false, expiresAt));

        assertThat(store.find(hash)).hasValue(new StoredRefreshToken(hash, family, "alice@example.com", false,
                expiresAt));
        Integer ttl = cassandraOperations.getCqlOperations().queryForObject(
                "SELECT TTL(email) FROM auth_refresh_tokens WHERE token_hash = ?", Integer.class, hash);
        assertThat(ttl).isBetween(3500, 3600);
    }

    @Test
    void marksATokenAsUsedExactlyOnce() {
        store.save(new StoredRefreshToken(hash, family, "alice@example.com", false, expiresAt));

        assertThat(store.markUsed(hash, expiresAt)).isTrue();
        assertThat(store.markUsed(hash, expiresAt)).isFalse();
        assertThat(store.find(hash)).hasValueSatisfying(token -> assertThat(token.used()).isTrue());
        Integer ttl = cassandraOperations.getCqlOperations().queryForObject(
                "SELECT TTL(used) FROM auth_refresh_tokens WHERE token_hash = ?", Integer.class, hash);
        assertThat(ttl).as("the used flag must expire with the token").isPositive();
    }

    @Test
    void revokesWholeFamilies() {
        assertThat(store.isFamilyRevoked(family)).isFalse();

        store.revokeFamily(family, expiresAt);

        assertThat(store.isFamilyRevoked(family)).isTrue();
        assertThat(store.isFamilyRevoked(UUID.randomUUID())).isFalse();
    }
}
