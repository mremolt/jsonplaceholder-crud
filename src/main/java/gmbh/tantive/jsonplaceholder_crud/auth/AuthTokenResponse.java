package gmbh.tantive.jsonplaceholder_crud.auth;

/**
 * Tokens of a session. The access token is a short-lived bearer JWT; the refresh token is opaque, single use and
 * must be exchanged at {@code /auth/refresh} for a new pair. Both lifetimes are in seconds.
 */
public record AuthTokenResponse(String accessToken, String tokenType, long expiresIn, String refreshToken,
        long refreshExpiresIn) {

    static AuthTokenResponse of(AccessToken accessToken, IssuedRefreshToken refreshToken) {
        return new AuthTokenResponse(accessToken.value(), "Bearer", accessToken.expiresIn(), refreshToken.value(),
                refreshToken.expiresIn());
    }
}
