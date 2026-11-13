package dev.ivanvoloshyn.theraflowapi.fixtures;

import java.util.UUID;

public class Constants {

    public static final String SIGN_UP_ENDPOINT = "/api/v1/auth/password/sign-up";
    public static final String VERIFY_EMAIL_ENDPOINT = "/api/v1/auth/verify-email";

    public final static String DEFAULT_EMAIL = "test@gmail.com";
    public final static String CORRECT_PASSWORD = "1123!Password";
    public static final String TEST_UUID = "cc837471-3c4b-4d77-a825-c4c1cf3a1dc5";

    public static final UUID ACCOUNT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    public static final UUID RANDOM_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
}


