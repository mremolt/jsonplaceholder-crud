package gmbh.tantive.jsonplaceholder_crud.auth;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** In-memory {@link RefreshTokenStore}; keeps expired entries, like Cassandra until the TTL removes them. */
class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private final Map<String, StoredRefreshToken> tokens = new HashMap<>();
    private final Set<UUID> revokedFamilies = new HashSet<>();

    @Override
    public void save(StoredRefreshToken token) {
        tokens.put(token.tokenHash(), token);
    }

    @Override
    public Optional<StoredRefreshToken> find(String tokenHash) {
        return Optional.ofNullable(tokens.get(tokenHash));
    }

    @Override
    public boolean markUsed(String tokenHash, Instant expiresAt) {
        StoredRefreshToken token = tokens.get(tokenHash);
        if (token == null || token.used()) {
            return false;
        }
        tokens.put(tokenHash, new StoredRefreshToken(token.tokenHash(), token.familyId(), token.email(), true,
                token.expiresAt()));
        return true;
    }

    @Override
    public void revokeFamily(UUID familyId, Instant until) {
        revokedFamilies.add(familyId);
    }

    @Override
    public boolean isFamilyRevoked(UUID familyId) {
        return revokedFamilies.contains(familyId);
    }

    Set<String> storedHashes() {
        return tokens.keySet();
    }
}
