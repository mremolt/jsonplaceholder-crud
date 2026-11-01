package gmbh.tantive.jsonplaceholder_crud.auth;

import java.util.Locale;

final class AuthEmail {

    private AuthEmail() {
    }

    static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}