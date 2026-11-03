package gmbh.tantive.jsonplaceholder_crud.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.cql.CqlOperations;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/** Stores refresh tokens with a Cassandra TTL, so expired tokens and revocations clean up themselves. */
@Repository
@RequiredArgsConstructor
public class CassandraRefreshTokenStore implements RefreshTokenStore {

    private final CassandraOperations cassandraOperations;
    private final Clock clock;

    @Override
    public void save(StoredRefreshToken token) {
        cql().execute("INSERT INTO auth_refresh_tokens (token_hash, family_id, email, used, expires_at) "
                + "VALUES (?, ?, ?, ?, ?) USING TTL ?", token.tokenHash(), token.familyId(), token.email(),
                token.used(), token.expiresAt(), ttlUntil(token.expiresAt()));
    }

    @Override
    public Optional<StoredRefreshToken> find(String tokenHash) {
        return cql().query("SELECT token_hash, family_id, email, used, expires_at FROM auth_refresh_tokens "
                + "WHERE token_hash = ?", (row, rowNum) -> new StoredRefreshToken(row.getString(0), row.getUuid(1),
                        row.getString(2), row.getBoolean(3), row.getInstant(4)), tokenHash)
                .stream()
                .findFirst();
    }

    // Lightweight transaction: of two concurrent rotations with the same token only one can succeed.
    @Override
    public boolean markUsed(String tokenHash, Instant expiresAt) {
        return cql().execute("UPDATE auth_refresh_tokens USING TTL ? SET used = true WHERE token_hash = ? "
                + "IF used = false", ttlUntil(expiresAt), tokenHash);
    }

    @Override
    public void revokeFamily(UUID familyId, Instant until) {
        cql().execute("INSERT INTO auth_revoked_refresh_families (family_id, revoked_at) VALUES (?, ?) USING TTL ?",
                familyId, clock.instant(), ttlUntil(until));
    }

    @Override
    public boolean isFamilyRevoked(UUID familyId) {
        return !cql().queryForList("SELECT family_id FROM auth_revoked_refresh_families WHERE family_id = ?",
                UUID.class, familyId).isEmpty();
    }

    private CqlOperations cql() {
        return cassandraOperations.getCqlOperations();
    }

    private int ttlUntil(Instant instant) {
        return (int) Math.max(1L, Duration.between(clock.instant(), instant).toSeconds());
    }
}
