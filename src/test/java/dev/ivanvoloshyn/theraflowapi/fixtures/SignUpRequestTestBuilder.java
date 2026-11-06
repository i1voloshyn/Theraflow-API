package dev.ivanvoloshyn.theraflowapi.fixtures;

import dev.ivanvoloshyn.theraflowapi.account.dto.AccountType;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.PasswordSignUpRequest;

import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.CORRECT_PASSWORD;
import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.DEFAULT_EMAIL;

public class SignUpRequestTestBuilder {

    public static PasswordSignUpRequest defaultTherapistSignUpRequest() {
        return new PasswordSignUpRequest(
                DEFAULT_EMAIL,
                AccountType.THERAPIST,
                CORRECT_PASSWORD
        );
    }
}
