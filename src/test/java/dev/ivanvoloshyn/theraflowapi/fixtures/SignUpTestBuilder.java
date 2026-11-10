package dev.ivanvoloshyn.theraflowapi.fixtures;

import dev.ivanvoloshyn.theraflowapi.account.dto.AccountStatus;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountType;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.PasswordSignUpRequest;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.SignUpResponse;

import java.util.UUID;

import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.CORRECT_PASSWORD;
import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.DEFAULT_EMAIL;

public class SignUpTestBuilder {

    public static PasswordSignUpRequest defaultTherapistSignUpRequest() {
        return new PasswordSignUpRequest(
                DEFAULT_EMAIL,
                AccountType.THERAPIST,
                CORRECT_PASSWORD
        );
    }

    public static PasswordSignUpRequest weakPasswordSignUpRequest() {
        return new PasswordSignUpRequest(
                DEFAULT_EMAIL,
                AccountType.THERAPIST,
                "weak_password"
        );
    }

    public static PasswordSignUpRequest invalidEmailSignUpRequest() {
        return new PasswordSignUpRequest(
                "invalid_email.com",
                AccountType.THERAPIST,
                CORRECT_PASSWORD
        );
    }

    public static SignUpResponse defaultTherapistSignUpResponse() {
        return new SignUpResponse(
                UUID.fromString(Constants.TEST_UUID),
                AccountType.THERAPIST,
                AccountStatus.PENDING_VERIFICATION
        );
    }
}
