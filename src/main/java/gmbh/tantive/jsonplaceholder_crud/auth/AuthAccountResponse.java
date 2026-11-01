package gmbh.tantive.jsonplaceholder_crud.auth;

import java.time.Instant;
import java.util.UUID;

public record AuthAccountResponse(UUID accountId, String email, String role, Instant createdAt) {

    public static AuthAccountResponse from(AuthAccount account) {
        return new AuthAccountResponse(account.getAccountId(), account.getEmail(), account.getRole(),
                account.getCreatedAt());
    }
}