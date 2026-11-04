package dev.ivanvoloshyn.theraflowapi.account;

import dev.ivanvoloshyn.theraflowapi.account.dto.AccountStatus;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountType;
import dev.ivanvoloshyn.theraflowapi.account.model.Account;

public class AccountTestBuilder {

    public static Account therapistUnverifiedAccount() {
        return new Account(AccountType.THERAPIST, AccountStatus.PENDING_VERIFICATION);
    }
}
