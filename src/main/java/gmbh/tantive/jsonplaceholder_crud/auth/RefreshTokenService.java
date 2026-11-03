package gmbh.tantive.jsonplaceholder_crud.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Issues and rotates opaque refresh tokens (OAuth 2.0 Security BCP): every token is single use, and presenting a
 * used token again revokes all tokens of its login, because a copy of it must exist outside the legitimate client.
 */
@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final SecureRandom secureRandom = new SecureRandom();
    private final RefreshTokenStore store;
    private final Clock clock;
    private final Duration ttl;

    public RefreshTokenService(RefreshTokenStore store, Clock clock,
            @Value("${security.jwt.refresh-token-ttl:P7D}") Duration ttl) {
        this.store = store;
        this.clock = clock;
        this.ttl = ttl;
    }

    public IssuedRefreshToken issue(String email) {
        return issue(email, UUID.randomUUID());
    }

    public RefreshTokenRotation rotate(String rawToken) {
        StoredRefreshToken token = store.find(hash(rawToken))
                .filter(candidate -> candidate.expiresAt().isAfter(clock.instant()))
                .orElseThrow(InvalidRefreshTokenException::new);
        if (store.isFamilyRevoked(token.familyId())) {
            throw new InvalidRefreshTokenException();
        }
        if (!store.markUsed(token.tokenHash(), token.expiresAt())) {
            store.revokeFamily(token.familyId(), revocationEnd());
            throw new InvalidRefreshTokenException();
        }
        return new RefreshTokenRotation(token.email(), issue(token.email(), token.familyId()));
    }

    public void revoke(String rawToken) {
        store.find(hash(rawToken)).ifPresent(token -> store.revokeFamily(token.familyId(), revocationEnd()));
    }

    private IssuedRefreshToken issue(String email, UUID familyId) {
        byte[] random = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(random);
        String value = ENCODER.encodeToString(random);
        store.save(new StoredRefreshToken(hash(value), familyId, email, false, clock.instant().plus(ttl)));
        return new IssuedRefreshToken(value, ttl.toSeconds());
    }

    // Every token of a family expires at the latest one TTL after now, so the revocation can expire then too.
    private Instant revocationEnd() {
        return clock.instant().plus(ttl);
    }

    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return ENCODER.encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
