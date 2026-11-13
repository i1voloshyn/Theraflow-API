package dev.ivanvoloshyn.theraflowapi.authentication;

import dev.ivanvoloshyn.theraflowapi.account.AccountService;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountStatus;
import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.PasswordSignUpRequest;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.SignUpResponse;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthIdentity;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthProvider;
import dev.ivanvoloshyn.theraflowapi.authentication.model.PasswordResetToken;
import dev.ivanvoloshyn.theraflowapi.event.model.EmailVerificationRequest;
import dev.ivanvoloshyn.theraflowapi.event.model.PasswordResetRequestEvent;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.PermissionException;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.ResourceConflictException;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import dev.ivanvoloshyn.theraflowapi.jwt.EmailVerificationTokenProvider;
import dev.ivanvoloshyn.theraflowapi.security.UUIDTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class PasswordAuthenticationService {
    private final AccountService accountService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AuthenticationMapper authenticationMapper;
    private final AuthIdentityRepository authIdentityRepository;
    private final EmailVerificationTokenProvider emailTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final Supplier<UUID> uuidSupplier;
    private final UUIDTokenProvider uuidTokenProvider;


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
        String token = emailTokenProvider.generateToken(identity.getEmail());
        applicationEventPublisher.publishEvent(new EmailVerificationRequest(
                identity.getId(),
                identity.getEmail(),
                token
        ));
    }

    @Transactional
    public void verifyEmail(String token) {
        final String email;
        try {
            email = emailTokenProvider.extractEmail(token);
        } catch (PermissionException e) {
            throw new PermissionException(ErrorCode.EMAIL_VERIFICATION_LINK_INVALID, e);
        }

        Account account = accountService.findByEmail(email, AuthProvider.PASSWORD);

        switch (account.getStatus()) {
            case ACTIVE -> throw new ResourceConflictException(ErrorCode.EMAIL_ALREADY_VERIFIED);
            case DISABLED -> throw new PermissionException(ErrorCode.ACCOUNT_DISABLED);
            case PENDING_VERIFICATION -> account.setStatus(AccountStatus.ACTIVE);
        }
    }

    @Transactional
    public void requestPasswordReset(String email) {
        Account account = accountService.findByEmail(email, AuthProvider.PASSWORD);

        String rawResetToken = uuidSupplier.get().toString();
        PasswordResetToken resetToken = uuidTokenProvider.buildPasswordResetToken(rawResetToken);

        account.addPasswordResetToken(resetToken);

        applicationEventPublisher.publishEvent(new PasswordResetRequestEvent(
                email,
                rawResetToken
        ));
    }

}
