package dev.ivanvoloshyn.theraflowapi.guardian;

import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import dev.ivanvoloshyn.theraflowapi.guardian.dto.SaveGuardianRequest;
import dev.ivanvoloshyn.theraflowapi.guardian.model.Guardian;

import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.ACCOUNT_ID;

public class GuardianTestBuilder {

    public static SaveGuardianRequest defaultSaveGuardianRequest() {
        return new SaveGuardianRequest("Joe", "Canada");
    }

    public static SaveGuardianRequest saveGuardianRequestWithSpaces() {
        return new SaveGuardianRequest(" Joe ", " Canada ");
    }

    public static Guardian savedGuardian(Account account) {
        return new Guardian(ACCOUNT_ID, "Joe", "Canada", null, null, account);
    }
}
