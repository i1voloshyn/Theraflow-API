package dev.ivanvoloshyn.theraflowapi.authentication;

import dev.ivanvoloshyn.theraflowapi.account.AccountService;
import dev.ivanvoloshyn.theraflowapi.account.AccountTestBuilder;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountStatus;
import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.ConfirmPasswordResetRequest;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.PasswordSignUpRequest;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.SignUpResponse;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthProvider;
import dev.ivanvoloshyn.theraflowapi.authentication.model.PasswordResetToken;
import dev.ivanvoloshyn.theraflowapi.event.model.EmailVerificationRequest;
import dev.ivanvoloshyn.theraflowapi.event.model.PasswordResetRequestEvent;
import dev.ivanvoloshyn.theraflowapi.exception.EntityNotFoundException;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.PermissionException;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.TheraflowApiException;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.ResourceConflictException;
import dev.ivanvoloshyn.theraflowapi.fixtures.SignUpTestBuilder;
import dev.ivanvoloshyn.theraflowapi.jwt.EmailVerificationTokenProvider;
import dev.ivanvoloshyn.theraflowapi.security.UUIDTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.CORRECT_PASSWORD;
import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.DEFAULT_EMAIL;
import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.RANDOM_UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordAuthenticationServiceTest {
    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);
    @Mock
    private AuthIdentityRepository authIdentityRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AccountService accountService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private EmailVerificationTokenProvider emailVerificationTokenProvider;
    @Spy
    private AuthenticationMapper authenticationMapper = Mappers.getMapper(AuthenticationMapper.class);
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @InjectMocks
    private PasswordAuthenticationService authenticationService;

    @Mock
    private UUIDTokenProvider uuidTokenProvider;
    @Mock
    private Supplier<UUID> uuidSupplier;

    @DisplayName("""
            signUp
            SHOULD throw ResourceConflictException
            WHEN provided email with AuthProvider already exists in DB
            """)
    @Test
    void signUp_error() {
        PasswordSignUpRequest request = SignUpTestBuilder.defaultTherapistSignUpRequest();

        when(authIdentityRepository.existsByProviderAndProviderUserId(AuthProvider.PASSWORD, request.email()))
                .thenReturn(true);

        assertThatExceptionOfType(ResourceConflictException.class)
                .isThrownBy(() -> authenticationService.signUp(request))
                .matches(ex -> ex.getErrorCode().equals(ErrorCode.EMAIL_ALREADY_EXISTS));

        verifyNoInteractions(passwordEncoder, accountService, applicationEventPublisher);

    }

    @DisplayName("""
            signUp
            SHOULD create new Account with Identity and
            PUBLISH sending verification email and
            RETURN SignUpResponse
            """)
    @Test
    void signUp_success() {
        PasswordSignUpRequest request = SignUpTestBuilder.defaultTherapistSignUpRequest();
        Account expectedAccount = AccountTestBuilder.therapistUnverifiedAccount();

        String passwordHash = "password-hash";
        String emailVerificationToken = "valid-jwt-token";
        EmailVerificationRequest emailVerificationRequest = new EmailVerificationRequest(
                expectedAccount.getId(), request.email(), emailVerificationToken);

        when(authIdentityRepository.existsByProviderAndProviderUserId(AuthProvider.PASSWORD, request.email()))
                .thenReturn(false);
        when(accountService.createAccount(request.type())).thenReturn(expectedAccount);
        when(passwordEncoder.encode(request.password())).thenReturn(passwordHash);
        when(emailVerificationTokenProvider.generateToken(request.email())).thenReturn(emailVerificationToken);

        SignUpResponse actualResponse = authenticationService.signUp(request);

        verify(authIdentityRepository).existsByProviderAndProviderUserId(AuthProvider.PASSWORD, request.email());
        verify(accountService).createAccount(request.type());
        verify(passwordEncoder).encode(request.password());
        verify(emailVerificationTokenProvider).generateToken(request.email());
        verify(applicationEventPublisher).publishEvent(emailVerificationRequest);

        assertThat(actualResponse.id()).isEqualTo(expectedAccount.getId());
        assertThat(actualResponse.status()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
    }

    @DisplayName("""
            verifyEmail
            SHOULD change account state to ACTIVE
            FOR unverifiedAccount
            WITH valid verification link
            """)
    @Test
    void verifyEmail() {
        String verificationToken = "cc837471-3c4b-4d77-a825-c4c1cf3a1dc5";
        Account account = AccountTestBuilder.therapistUnverifiedAccount();

        when(emailVerificationTokenProvider.extractEmail(verificationToken)).thenReturn(DEFAULT_EMAIL);
        when(accountService.findByEmailAndProvider(DEFAULT_EMAIL, AuthProvider.PASSWORD)).thenReturn(account);

        authenticationService.verifyEmail(verificationToken);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);

        verify(emailVerificationTokenProvider).extractEmail(verificationToken);
        verify(accountService).findByEmailAndProvider(DEFAULT_EMAIL, AuthProvider.PASSWORD);
    }

    @DisplayName("""
            Verified email with expired tokens
            Should thrown InvalidCredentialsException
            With EMAIL_VERIFICATION_LINK_EXPIRED error code
            """)
    @Test
    void verifyEmail_error1() {
        String expiredVerificationToken = "cc837471-3c4b-4d77-a825-c4c1cf3a1dc5";

        when(emailVerificationTokenProvider.extractEmail(expiredVerificationToken))
                .thenThrow(new PermissionException(ErrorCode.TOKEN_EXPIRED));

        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> authenticationService.verifyEmail(expiredVerificationToken))
                .matches(ex -> ex.getErrorCode().equals(ErrorCode.EMAIL_VERIFICATION_LINK_INVALID));
    }

    @DisplayName("""
            verifyEmail
            WITH already-used verification link
            SHOULD throw ResourceConflictException
            WITH code-EMAIL_ALREADY_VERIFIED
            """)
    @Test
    void verifyEmail_error2() {
        String verificationToken = "cc837471-3c4b-4d77-a825-c4c1cf3a1dc5";
        Account verifiedAccount = AccountTestBuilder.therapistVerifiedAccount();

        when(emailVerificationTokenProvider.extractEmail(verificationToken)).thenReturn(DEFAULT_EMAIL);
        when(accountService.findByEmailAndProvider(DEFAULT_EMAIL, AuthProvider.PASSWORD)).thenReturn(verifiedAccount);

        assertThatExceptionOfType(ResourceConflictException.class)
                .isThrownBy(() -> authenticationService.verifyEmail(verificationToken))
                .matches(ex -> ex.getErrorCode().equals(ErrorCode.EMAIL_ALREADY_VERIFIED));

        verify(emailVerificationTokenProvider).extractEmail(verificationToken);
        verify(accountService).findByEmailAndProvider(DEFAULT_EMAIL, AuthProvider.PASSWORD);
    }

    @DisplayName("""
            requestPasswordReset
            SHOULD NOT create a token
            AND SHOULD NOT send an email
            WHEN the email does not exist in the database
            """)
    @Test
    void requestPasswordReset_error() {
        String nonExistentEmail = "wrong-email";

        when(accountService
                .findByEmailAndProvider(nonExistentEmail, AuthProvider.PASSWORD)).thenThrow(EntityNotFoundException.class);

        authenticationService.requestPasswordReset(nonExistentEmail);

        verifyNoInteractions(uuidSupplier, uuidTokenProvider, applicationEventPublisher);
    }

    @DisplayName("""
            requestPasswordReset
            SHOULD create a password reset token
            AND publish a password reset event
            WHEN an account exists for the provided email
            """)
    @Test
    void requestPasswordReset_success() {
        Account account = AccountTestBuilder.therapistVerifiedAccount();
        String email = DEFAULT_EMAIL;
        String rawResetToken = RANDOM_UUID.toString();
        String tokenHash = "valid-token-hash";
        PasswordResetToken resetToken = new PasswordResetToken(tokenHash,
                clock.instant().plusSeconds(3600));

        PasswordResetRequestEvent resetRequestEvent = new PasswordResetRequestEvent(DEFAULT_EMAIL, rawResetToken);

        when(accountService.findByEmailAndProvider(email, AuthProvider.PASSWORD)).thenReturn(account);
        when(uuidSupplier.get()).thenReturn(RANDOM_UUID);
        when(uuidTokenProvider.buildPasswordResetToken(rawResetToken)).thenReturn(resetToken);

        authenticationService.requestPasswordReset(DEFAULT_EMAIL);

        verify(applicationEventPublisher).publishEvent(resetRequestEvent);

        assertThat(account.getPasswordResetTokens())
                .containsExactly(resetToken);

    }

    @DisplayName("""
            validatePasswordResetLink
            SHOULD throw TheraflowApiException
            WITH PASSWORD_RESET_TOKEN_INVALID
            WHEN token is expired
            """)
    @Test
    void validatePasswordResetLink_error() {
        String rawResetToken = RANDOM_UUID.toString();
        String tokenHash = "valid-token-hash";
        PasswordResetToken expiredToken = new PasswordResetToken(tokenHash,
                clock.instant().minusSeconds(1));

        when(uuidTokenProvider.hashToken(rawResetToken)).thenReturn(tokenHash);
        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(expiredToken));

        assertThatExceptionOfType(TheraflowApiException.class)
                .isThrownBy(() -> authenticationService.validatePasswordResetLink(rawResetToken))
                .matches(ex -> ex.getErrorCode().equals(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));

    }

    @DisplayName("""
            validatePasswordResetLink
            SHOULD accept the link without consuming the token
            WHEN token is valid and unused
            """)
    @Test
    void validatePasswordResetLink_success() {
        String rawResetToken = RANDOM_UUID.toString();
        String tokenHash = "valid-token-hash";
        PasswordResetToken resetToken = new PasswordResetToken(tokenHash,
                clock.instant().plusSeconds(3600));

        when(uuidTokenProvider.hashToken(rawResetToken)).thenReturn(tokenHash);
        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));

        authenticationService.validatePasswordResetLink(rawResetToken);

        assertThat(resetToken.isUsed()).isFalse();
        verifyNoInteractions(passwordEncoder);
    }

    @DisplayName("""
            confirmPasswordReset
            SHOULD update the password hash
            AND mark the token as used
            WHEN token is valid and unused
            """)
    @Test
    void confirmPasswordReset_success() {
        String rawResetToken = RANDOM_UUID.toString();
        String tokenHash = "valid-token-hash";
        String newPasswordHash = "new-password-hash";
        ConfirmPasswordResetRequest request = new ConfirmPasswordResetRequest(
                rawResetToken, CORRECT_PASSWORD, CORRECT_PASSWORD);
        Account account = AccountTestBuilder.therapistVerifiedAccount();
        account.setAuthIdentity(AuthIdentityTestBuilder.passwordProviderAuthIdentity());
        PasswordResetToken resetToken = new PasswordResetToken(tokenHash,
                clock.instant().plusSeconds(3600));
        account.addPasswordResetToken(resetToken);

        when(uuidTokenProvider.hashToken(rawResetToken)).thenReturn(tokenHash);
        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode(request.newPassword())).thenReturn(newPasswordHash);

        authenticationService.confirmPasswordReset(request);

        assertThat(account.getAuthIdentity().getPasswordHash()).isEqualTo(newPasswordHash);
        assertThat(resetToken.isUsed()).isTrue();
        verify(passwordEncoder).encode(request.newPassword());
    }

    @DisplayName("""
            confirmPasswordReset
            SHOULD throw TheraflowApiException
            WITH PASSWORD_RESET_TOKEN_INVALID
            WHEN token is expired
            """)
    @Test
    void confirmPasswordReset_error1() {
        String rawResetToken = RANDOM_UUID.toString();
        String tokenHash = "valid-token-hash";
        ConfirmPasswordResetRequest request = new ConfirmPasswordResetRequest(
                rawResetToken, CORRECT_PASSWORD, CORRECT_PASSWORD);
        Account account = AccountTestBuilder.therapistVerifiedAccount();
        account.setAuthIdentity(AuthIdentityTestBuilder.passwordProviderAuthIdentity());
        String originalPasswordHash = account.getAuthIdentity().getPasswordHash();
        PasswordResetToken expiredToken = new PasswordResetToken(tokenHash,
                clock.instant().minusSeconds(1));
        account.addPasswordResetToken(expiredToken);

        when(uuidTokenProvider.hashToken(rawResetToken)).thenReturn(tokenHash);
        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(expiredToken));

        assertThatExceptionOfType(TheraflowApiException.class)
                .isThrownBy(() -> authenticationService.confirmPasswordReset(request))
                .matches(ex -> ex.getErrorCode().equals(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));

        assertThat(account.getAuthIdentity().getPasswordHash()).isEqualTo(originalPasswordHash);
        assertThat(expiredToken.isUsed()).isFalse();
        verifyNoInteractions(passwordEncoder);
    }

    @DisplayName("""
            confirmPasswordReset
            SHOULD throw TheraflowApiException
            WITH PASSWORD_RESET_TOKEN_INVALID
            WHEN token is already used
            """)
    @Test
    void confirmPasswordReset_error2() {
        String rawResetToken = RANDOM_UUID.toString();
        String tokenHash = "valid-token-hash";
        ConfirmPasswordResetRequest request = new ConfirmPasswordResetRequest(
                rawResetToken, CORRECT_PASSWORD, CORRECT_PASSWORD);
        Account account = AccountTestBuilder.therapistVerifiedAccount();
        account.setAuthIdentity(AuthIdentityTestBuilder.passwordProviderAuthIdentity());
        String originalPasswordHash = account.getAuthIdentity().getPasswordHash();
        PasswordResetToken usedToken = new PasswordResetToken(tokenHash,
                clock.instant().plusSeconds(3600));
        usedToken.setUsed(true);
        account.addPasswordResetToken(usedToken);

        when(uuidTokenProvider.hashToken(rawResetToken)).thenReturn(tokenHash);
        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(usedToken));

        assertThatExceptionOfType(TheraflowApiException.class)
                .isThrownBy(() -> authenticationService.confirmPasswordReset(request))
                .matches(ex -> ex.getErrorCode().equals(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));

        assertThat(account.getAuthIdentity().getPasswordHash()).isEqualTo(originalPasswordHash);
        assertThat(usedToken.isUsed()).isTrue();
        verifyNoInteractions(passwordEncoder);
    }

    @DisplayName("""
            confirmPasswordReset
            SHOULD throw TheraflowApiException
            WITH PASSWORD_RESET_TOKEN_INVALID
            WHEN token does not exist
            """)
    @Test
    void confirmPasswordReset_error3() {
        String rawResetToken = RANDOM_UUID.toString();
        String tokenHash = "unknown-token-hash";
        ConfirmPasswordResetRequest request = new ConfirmPasswordResetRequest(
                rawResetToken, CORRECT_PASSWORD, CORRECT_PASSWORD);

        when(uuidTokenProvider.hashToken(rawResetToken)).thenReturn(tokenHash);
        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

        assertThatExceptionOfType(TheraflowApiException.class)
                .isThrownBy(() -> authenticationService.confirmPasswordReset(request))
                .matches(ex -> ex.getErrorCode().equals(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));

        verifyNoInteractions(passwordEncoder);
    }

    @DisplayName("""
            confirmPasswordReset
            SHOULD throw TheraflowApiException
            WITH PASSWORD_RESET_TOKEN_INVALID
            WHEN token expires exactly at the current time
            """)
    @Test
    void confirmPasswordReset_error4() {
        String rawResetToken = RANDOM_UUID.toString();
        String tokenHash = "valid-token-hash";
        ConfirmPasswordResetRequest request = new ConfirmPasswordResetRequest(
                rawResetToken, CORRECT_PASSWORD, CORRECT_PASSWORD);
        Account account = AccountTestBuilder.therapistVerifiedAccount();
        account.setAuthIdentity(AuthIdentityTestBuilder.passwordProviderAuthIdentity());
        String originalPasswordHash = account.getAuthIdentity().getPasswordHash();
        PasswordResetToken expiredToken = new PasswordResetToken(tokenHash, clock.instant());
        account.addPasswordResetToken(expiredToken);

        when(uuidTokenProvider.hashToken(rawResetToken)).thenReturn(tokenHash);
        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(expiredToken));

        assertThatExceptionOfType(TheraflowApiException.class)
                .isThrownBy(() -> authenticationService.confirmPasswordReset(request))
                .matches(ex -> ex.getErrorCode().equals(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));

        assertThat(account.getAuthIdentity().getPasswordHash()).isEqualTo(originalPasswordHash);
        assertThat(expiredToken.isUsed()).isFalse();
        verifyNoInteractions(passwordEncoder);
    }

}
