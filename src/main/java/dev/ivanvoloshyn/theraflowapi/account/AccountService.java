package dev.ivanvoloshyn.theraflowapi.account;

import dev.ivanvoloshyn.theraflowapi.account.dto.AccountStatus;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountType;
import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthProvider;
import dev.ivanvoloshyn.theraflowapi.exception.EntityNotFoundException;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;

    public Account createAccount(AccountType type) {
        return accountRepository.save(new Account(type, AccountStatus.PENDING_VERIFICATION));
    }

    public Account findById(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    public Account findByIdForUpdate(UUID id) {
        return accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    public Account findByEmailAndProvider(String email, AuthProvider provider) {
        return accountRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));
    }
}
