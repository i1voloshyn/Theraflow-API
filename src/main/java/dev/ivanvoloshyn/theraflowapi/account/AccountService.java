package dev.ivanvoloshyn.theraflowapi.account;

import dev.ivanvoloshyn.theraflowapi.account.dto.AccountStatus;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountType;
import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import dev.ivanvoloshyn.theraflowapi.exception.EntityNotFoundException;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;

    public Account createAccount(AccountType type) {
        return accountRepository.save(new Account(type, AccountStatus.PENDING_VERIFICATION));
    }

    public Account findByEmail(String email) {
        return accountRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));
    }
}
