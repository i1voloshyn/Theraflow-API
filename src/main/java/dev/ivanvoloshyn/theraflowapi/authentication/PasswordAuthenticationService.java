package dev.ivanvoloshyn.theraflowapi.authentication;

import dev.ivanvoloshyn.theraflowapi.account.AccountService;
import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.PasswordSignUpRequest;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthIdentity;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthProvider;
import dev.ivanvoloshyn.theraflowapi.event.EmailVerificationRequest;
import dev.ivanvoloshyn.theraflowapi.exception.ErrorCode;
import dev.ivanvoloshyn.theraflowapi.exception.ResourceConflictException;
import dev.ivanvoloshyn.theraflowapi.exception.TheraflowApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordAuthenticationService {
    private final AuthIdentityRepository authIdentityRepository;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public void signUp(PasswordSignUpRequest request) {
        try {
            if (authIdentityRepository.existsByProviderAndProviderUserId(AuthProvider.PASSWORD, request.email())) {
                throw new ResourceConflictException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            Account account = accountService.createAccount(request.type());

            String passwordHash = passwordEncoder.encode(request.password());

            AuthIdentity authIdentity = new AuthIdentity(AuthProvider.PASSWORD, request.email(), request.email(), passwordHash);
            account.setAuthIdentity(authIdentity);
       //     authIdentityRepository.save(authIdentity);

            applicationEventPublisher.publishEvent(new EmailVerificationRequest()); //todo sending email is not yet implemented
        } catch (Exception e) {
            //todo implement logger
            System.out.println(e.getMessage());
            throw new TheraflowApiException(ErrorCode.THERAFLOW_ERROR, e);
        }
    }
}
