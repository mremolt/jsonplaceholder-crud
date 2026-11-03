package gmbh.tantive.jsonplaceholder_crud.auth;

/** A newly issued refresh token; {@code value} is only ever returned to the client, never stored. */
public record IssuedRefreshToken(String value, long expiresIn) {
}
