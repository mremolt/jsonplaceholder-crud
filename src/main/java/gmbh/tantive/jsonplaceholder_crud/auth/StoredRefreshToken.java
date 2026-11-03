package gmbh.tantive.jsonplaceholder_crud.auth;

import java.time.Instant;
import java.util.UUID;

/**
 * A refresh token as stored: only the SHA-256 hash of the token value. All tokens rotated from one login share
 * the family id, so a detected reuse can end the whole session.
 */
public record StoredRefreshToken(String tokenHash, UUID familyId, String email, boolean used, Instant expiresAt) {
}
