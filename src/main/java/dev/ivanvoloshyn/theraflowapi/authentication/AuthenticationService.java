package dev.ivanvoloshyn.theraflowapi.authentication;

import dev.ivanvoloshyn.theraflowapi.account.AccountService;
import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.SignUpRequest;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthProvider;
import dev.ivanvoloshyn.theraflowapi.exception.ErrorCode;
import dev.ivanvoloshyn.theraflowapi.exception.ResourceConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final AuthIdentityRepository authIdentityRepository;
    private final AccountService accountService;

    @Transactional
    public void signUp(SignUpRequest request) {
        if (authIdentityRepository.existsByProviderAndProviderUserId(AuthProvider.PASSWORD, request.email())) {
            throw new ResourceConflictException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        Account account = accountService.createAccount(request.type());
    }
}
