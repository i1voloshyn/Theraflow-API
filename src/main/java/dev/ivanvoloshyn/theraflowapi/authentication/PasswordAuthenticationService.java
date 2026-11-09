package dev.ivanvoloshyn.theraflowapi.authentication;

import dev.ivanvoloshyn.theraflowapi.account.AccountService;
import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.SignUpResponse;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.PasswordSignUpRequest;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthIdentity;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthProvider;
import dev.ivanvoloshyn.theraflowapi.event.EmailVerificationRequest;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.ResourceConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PasswordAuthenticationService {
    private final AuthIdentityRepository authIdentityRepository;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AuthenticationMapper authenticationMapper;

    @Transactional
    public SignUpResponse signUp(PasswordSignUpRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (authIdentityRepository.existsByProviderAndProviderUserId(AuthProvider.PASSWORD, normalizedEmail)) {
            throw new ResourceConflictException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        Account account = accountService.createAccount(request.type());

        String passwordHash = passwordEncoder.encode(request.password());

        AuthIdentity authIdentity = new AuthIdentity(AuthProvider.PASSWORD, normalizedEmail, normalizedEmail, passwordHash);
        account.setAuthIdentity(authIdentity);

        publishEmailVerificationEvent(authIdentity);

        return authenticationMapper.toSignUpResponse(account);
    }

    private void publishEmailVerificationEvent(AuthIdentity identity) {
        String token = "jwt-token"; //todo
        applicationEventPublisher.publishEvent(new EmailVerificationRequest(
                identity.getId(),
                identity.getEmail(),
                token
        ));
    }
}
