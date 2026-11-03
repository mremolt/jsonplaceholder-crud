package gmbh.tantive.jsonplaceholder_crud.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import gmbh.tantive.jsonplaceholder_crud.support.MutableClock;
import org.junit.jupiter.api.Test;

class RefreshTokenServiceTest {

    private static final Duration TTL = Duration.ofDays(7);

    private final InMemoryRefreshTokenStore store = new InMemoryRefreshTokenStore();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-03T10:00:00Z"));
    private final RefreshTokenService service = new RefreshTokenService(store, clock, TTL);

    @Test
    void issuesAnOpaqueTokenAndStoresOnlyItsHash() {
        IssuedRefreshToken token = service.issue("alice@example.com");

        assertThat(token.value()).hasSizeGreaterThanOrEqualTo(43);
        assertThat(token.expiresIn()).isEqualTo(TTL.toSeconds());
        assertThat(store.storedHashes()).hasSize(1).doesNotContain(token.value());
    }

    @Test
    void rotationReturnsTheAccountAndANewSingleUseToken() {
        IssuedRefreshToken first = service.issue("alice@example.com");

        RefreshTokenRotation rotation = service.rotate(first.value());

        assertThat(rotation.email()).isEqualTo("alice@example.com");
        assertThat(rotation.next().value()).isNotEqualTo(first.value());
        assertThat(service.rotate(rotation.next().value()).email()).isEqualTo("alice@example.com");
    }

    @Test
    void reusingARotatedTokenRevokesTheWholeFamily() {
        IssuedRefreshToken stolen = service.issue("alice@example.com");
        IssuedRefreshToken legitimate = service.rotate(stolen.value()).next();

        assertThatThrownBy(() -> service.rotate(stolen.value())).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> service.rotate(legitimate.value())).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rejectsExpiredAndUnknownTokens() {
        IssuedRefreshToken token = service.issue("alice@example.com");
        clock.advance(TTL);

        assertThatThrownBy(() -> service.rotate(token.value())).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> service.rotate("unknown")).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void revokeEndsTheSessionAndIgnoresUnknownTokens() {
        IssuedRefreshToken token = service.issue("alice@example.com");
        IssuedRefreshToken next = service.rotate(token.value()).next();

        service.revoke(next.value());

        assertThatThrownBy(() -> service.rotate(next.value())).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatNoException().isThrownBy(() -> service.revoke("unknown"));
    }
}
