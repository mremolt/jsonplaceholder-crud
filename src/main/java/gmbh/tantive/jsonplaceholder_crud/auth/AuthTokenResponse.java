package gmbh.tantive.jsonplaceholder_crud.auth;

public record AuthTokenResponse(String accessToken, String tokenType, long expiresIn) {
}