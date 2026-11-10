package dev.ivanvoloshyn.theraflowapi.authentication;

import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthIdentity;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthProvider;

import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.DEFAULT_EMAIL;

public class AuthIdentityTestBuilder {

    private final static AuthProvider PASSWORD_PROVIDER = AuthProvider.PASSWORD;
    private final static String PASSWORD_HASH = "password-hash";

    static AuthIdentity passwordProviderAuthIdentity() {
        return new AuthIdentity(PASSWORD_PROVIDER, DEFAULT_EMAIL, DEFAULT_EMAIL, PASSWORD_HASH);
    }
}
