package dev.ivanvoloshyn.theraflowapi.authentication;

import dev.ivanvoloshyn.theraflowapi.account.AccountService;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountStatus;
import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.ConfirmPasswordResetRequest;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.JwtPair;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.PasswordSignInRequest;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.PasswordSignUpRequest;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.SignUpResponse;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthIdentity;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthProvider;
import dev.ivanvoloshyn.theraflowapi.authentication.model.PasswordResetToken;
import dev.ivanvoloshyn.theraflowapi.authentication.model.RefreshToken;
import dev.ivanvoloshyn.theraflowapi.event.model.EmailVerificationRequest;
import dev.ivanvoloshyn.theraflowapi.event.model.PasswordResetRequestEvent;
import dev.ivanvoloshyn.theraflowapi.exception.EntityNotFoundException;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.PermissionException;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.ResourceConflictException;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.TheraflowApiException;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import dev.ivanvoloshyn.theraflowapi.jwt.EmailVerificationTokenProvider;
import dev.ivanvoloshyn.theraflowapi.jwt.JwtAuthTokenProvider;
import dev.ivanvoloshyn.theraflowapi.security.UUIDTokenProvider;
import dev.ivanvoloshyn.theraflowapi.security.model.TheraflowUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordAuthenticationService {
    private final AccountService accountService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AuthenticationMapper authenticationMapper;
    private final AuthIdentityRepository authIdentityRepository;
    private final EmailVerificationTokenProvider emailTokenProvider;
    private final PasswordEncoder passwordEncoder;
    // maybe in future it can be replaced with service but for now just leave it
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final Supplier<UUID> uuidSupplier;
    private final UUIDTokenProvider uuidTokenProvider;
    private final Clock clock;
    private final AuthenticationManager authenticationManager;
    private final JwtAuthTokenProvider jwtAuthTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public JwtPair signIn(PasswordSignInRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        Account account = authIdentityRepository
                .findAccountIdByProviderAndProviderUserId(AuthProvider.PASSWORD, normalizedEmail)
                .map(id -> lockAccount(id, ErrorCode.AUTHENTICATION_FAILED))
                .orElse(null);
        final Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(normalizedEmail, request.password()));
        } catch (BadCredentialsException | UsernameNotFoundException e) {
            throw new PermissionException(ErrorCode.AUTHENTICATION_FAILED, e);
        }

        if (!(authentication.getPrincipal() instanceof TheraflowUser user)) {
            throw new AuthenticationServiceException("Unexpected authentication principal type");
        }

        if (account == null || !account.getId().equals(user.getAccountId())) {
            throw new PermissionException(ErrorCode.AUTHENTICATION_FAILED);
        }
        validateAccountStatus(account);
        return issueTokens(account);
    }

    @Transactional
    public JwtPair rotateTokens(String rawRefreshToken) {
        String tokenHash = uuidTokenProvider.hashToken(rawRefreshToken);
        UUID accountId = refreshTokenRepository.findAccountIdByTokenHash(tokenHash)
                .orElseThrow(() -> new PermissionException(ErrorCode.REFRESH_TOKEN_INVALID));
        Account account = lockAccount(accountId, ErrorCode.REFRESH_TOKEN_INVALID);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new PermissionException(ErrorCode.REFRESH_TOKEN_INVALID));

        if (refreshToken.isRevoked() || !refreshToken.getExpiresAt().isAfter(clock.instant())) {
            throw new PermissionException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        validateAccountStatus(account);
        JwtPair tokens = issueTokens(account);
        refreshToken.setRevoked(true);
        return tokens;
    }

    private Account lockAccount(UUID accountId, ErrorCode notFoundCode) {
        // Lock the account before token rows so resets, sign-in and rotation use one lock order.
        try {
            return accountService.findByIdForUpdate(accountId);
        } catch (EntityNotFoundException e) {
            throw new PermissionException(notFoundCode, e);
        }
    }

    private void validateAccountStatus(Account account) {
        switch (account.getStatus()) {
            case PENDING_VERIFICATION -> throw new PermissionException(ErrorCode.EMAIL_NOT_VERIFIED);
            case DISABLED -> throw new PermissionException(ErrorCode.ACCOUNT_DISABLED);
            case ACTIVE -> { }
        }
    }

    private JwtPair issueTokens(Account account) {
        TheraflowUser user = new TheraflowUser(account.getId(), account.getAuthIdentity().getEmail(),
                null, account.getType(), account.getStatus());
        String accessToken = jwtAuthTokenProvider.generateAccessToken(user);
        String rawRefreshToken = uuidSupplier.get().toString();
        account.addRefreshToken(uuidTokenProvider.buildRefreshToken(rawRefreshToken));
        return new JwtPair(accessToken, rawRefreshToken);
    }

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

        Account account = accountService.findByEmailAndProvider(email, AuthProvider.PASSWORD);

        switch (account.getStatus()) {
            case ACTIVE -> throw new ResourceConflictException(ErrorCode.EMAIL_ALREADY_VERIFIED);
            case DISABLED -> throw new PermissionException(ErrorCode.ACCOUNT_DISABLED);
            case PENDING_VERIFICATION -> account.setStatus(AccountStatus.ACTIVE);
        }
    }

    @Transactional
    public void requestPasswordReset(String email) {
        final Account account;
        try {
            account = accountService.findByEmailAndProvider(email, AuthProvider.PASSWORD);
            if (!account.getStatus().equals(AccountStatus.ACTIVE)) {
                return;
            }
        } catch (EntityNotFoundException e) {
            return;
        }

        String rawResetToken = uuidSupplier.get().toString();
        PasswordResetToken resetToken = uuidTokenProvider.buildPasswordResetToken(rawResetToken);

        account.addPasswordResetToken(resetToken);

        applicationEventPublisher.publishEvent(new PasswordResetRequestEvent(
                email,
                rawResetToken
        ));
    }

    public void validatePasswordResetLink(String rawToken) {
        findValidPasswordResetToken(uuidTokenProvider.hashToken(rawToken));
    }

    @Transactional
    public void confirmPasswordReset(ConfirmPasswordResetRequest request) {
        String tokenHash = uuidTokenProvider.hashToken(request.token());
        UUID accountId = passwordResetTokenRepository.findAccountIdByTokenHash(tokenHash)
                .orElseThrow(() -> new PermissionException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));
        Account account = lockAccount(accountId, ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        PasswordResetToken resetToken = findValidPasswordResetToken(tokenHash);

        account.getAuthIdentity().setPasswordHash(passwordEncoder.encode(request.newPassword()));
        resetToken.setUsed(true);
        account.getRefreshTokens().forEach(token -> token.setRevoked(true));
    }

    private PasswordResetToken findValidPasswordResetToken(String tokenHash) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new TheraflowApiException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));

        validateToken(resetToken);
        return resetToken;
    }

    private void validateToken(PasswordResetToken resetToken) {
        if (resetToken.isUsed() || !resetToken.getExpiresAt().isAfter(clock.instant())) {
            throw new PermissionException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }
    }

}
