package dev.ivanvoloshyn.theraflowapi.fixtures;

import dev.ivanvoloshyn.theraflowapi.account.dto.AccountType;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.SignUpRequest;

import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.CORRECT_PASSWORD;
import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.DEFAULT_EMAIL;

public class SignUpRequestTestBuilder {

    public static SignUpRequest defaultTherapistSignUpRequest() {
        return new SignUpRequest(
                DEFAULT_EMAIL,
                AccountType.THERAPIST,
                CORRECT_PASSWORD
        );
    }
}
