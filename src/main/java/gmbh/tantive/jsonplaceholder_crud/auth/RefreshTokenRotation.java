package gmbh.tantive.jsonplaceholder_crud.auth;

public record RefreshTokenRotation(String email, IssuedRefreshToken next) {
}
