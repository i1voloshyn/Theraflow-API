package dev.ivanvoloshyn.theraflowapi.authentication;

import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthIdentity;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthProvider;

public class AuthIdentityTestBuilder {

    private final static AuthProvider PASSWORD_PROVIDER = AuthProvider.PASSWORD;
    private final static String DEFAULT_EMAIL = "test@gmail.com";
    private final static String PASSWORD_HASH = "password-hash";

    static AuthIdentity passwordProviderAuthIdentity() {
        return new AuthIdentity(PASSWORD_PROVIDER, DEFAULT_EMAIL, DEFAULT_EMAIL, PASSWORD_HASH);
    }
}
