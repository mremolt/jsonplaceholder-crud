package gmbh.tantive.jsonplaceholder_crud.auth;

import java.util.Optional;

public interface AuthAccountStore {

    Optional<AuthAccount> findByEmail(String normalizedEmail);

    boolean insertIfAbsent(AuthAccount account);
}