package gmbh.tantive.jsonplaceholder_crud.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Storage port for refresh tokens; entries may disappear once they expire. */
public interface RefreshTokenStore {

    void save(StoredRefreshToken token);

    Optional<StoredRefreshToken> find(String tokenHash);

    /** Marks the token as used; returns false if it was already used or does not exist. Must be atomic. */
    boolean markUsed(String tokenHash, Instant expiresAt);

    /** Revokes all tokens of the family; the revocation must be kept at least until {@code until}. */
    void revokeFamily(UUID familyId, Instant until);

    boolean isFamilyRevoked(UUID familyId);
}
