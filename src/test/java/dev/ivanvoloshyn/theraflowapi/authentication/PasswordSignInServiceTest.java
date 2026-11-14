package dev.ivanvoloshyn.theraflowapi.authentication;

import dev.ivanvoloshyn.theraflowapi.account.AccountService;
import dev.ivanvoloshyn.theraflowapi.account.AccountTestBuilder;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountStatus;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountType;
import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.JwtPair;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.PasswordSignInRequest;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthIdentity;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthProvider;
import dev.ivanvoloshyn.theraflowapi.authentication.model.RefreshToken;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.PermissionException;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import dev.ivanvoloshyn.theraflowapi.jwt.JwtAuthTokenProvider;
import dev.ivanvoloshyn.theraflowapi.security.UUIDTokenProvider;
import dev.ivanvoloshyn.theraflowapi.security.model.TheraflowUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.ACCOUNT_ID;
import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.CORRECT_PASSWORD;
import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.DEFAULT_EMAIL;
import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.RANDOM_UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordSignInServiceTest {

    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private AccountService accountService;
    @Mock
    private AuthIdentityRepository authIdentityRepository;
    @Mock
    private JwtAuthTokenProvider jwtAuthTokenProvider;
    @Mock
    private UUIDTokenProvider uuidTokenProvider;
    @Mock
    private Supplier<UUID> uuidSupplier;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @InjectMocks
    private PasswordAuthenticationService authenticationService;

    @DisplayName("""
            signIn
            SHOULD authenticate the normalized email and return a token pair
            AND attach the hashed refresh token to the account
            WHEN credentials are valid and the account is active
            """)
    @Test
    void signIn_success() {
        PasswordSignInRequest request = new PasswordSignInRequest("  TEST@GMAIL.COM  ", CORRECT_PASSWORD);
        Account account = AccountTestBuilder.therapistVerifiedAccount();
        ReflectionTestUtils.setField(account, "id", ACCOUNT_ID);
        account.setAuthIdentity(new AuthIdentity(AuthProvider.PASSWORD, DEFAULT_EMAIL, DEFAULT_EMAIL, "password-hash"));
        TheraflowUser user = authenticatedUser();
        String rawRefreshToken = RANDOM_UUID.toString();
        RefreshToken refreshToken = new RefreshToken("refresh-token-hash", clock.instant().plusSeconds(3600));

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        mockLockedSignInAccount(account);
        when(jwtAuthTokenProvider.generateAccessToken(any(TheraflowUser.class))).thenReturn("access-token");
        when(uuidSupplier.get()).thenReturn(RANDOM_UUID);
        when(uuidTokenProvider.buildRefreshToken(rawRefreshToken)).thenReturn(refreshToken);

        JwtPair response = authenticationService.signIn(request);

        assertThat(response).isEqualTo(new JwtPair("access-token", rawRefreshToken));
        assertThat(account.getRefreshTokens()).containsExactly(refreshToken);
        assertThat(refreshToken.getAccount()).isSameAs(account);
        assertThat(refreshToken.getTokenHash()).isEqualTo("refresh-token-hash");
        assertThat(refreshToken.isRevoked()).isFalse();

        ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(authenticationCaptor.capture());
        Authentication authentication = authenticationCaptor.getValue();
        assertThat(authentication.getPrincipal()).isEqualTo(DEFAULT_EMAIL);
        assertThat(authentication.getCredentials()).isEqualTo(CORRECT_PASSWORD);
        assertThat(authentication.isAuthenticated()).isFalse();
        ArgumentCaptor<TheraflowUser> userCaptor = ArgumentCaptor.forClass(TheraflowUser.class);
        verify(jwtAuthTokenProvider).generateAccessToken(userCaptor.capture());
        assertThat(userCaptor.getValue().getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(userCaptor.getValue().getUsername()).isEqualTo(DEFAULT_EMAIL);
        assertThat(userCaptor.getValue().getPassword()).isNull();
        var order = inOrder(accountService, authenticationManager);
        order.verify(accountService).findByIdForUpdate(ACCOUNT_ID);
        order.verify(authenticationManager).authenticate(any(Authentication.class));
    }

    @DisplayName("""
            signIn
            SHOULD throw PermissionException with AUTHENTICATION_FAILED
            AND SHOULD NOT issue tokens
            WHEN the password is incorrect
            """)
    @Test
    void signIn_error() {
        PasswordSignInRequest request = new PasswordSignInRequest(DEFAULT_EMAIL, "incorrect-password");
        mockLockedSignInAccount(AccountTestBuilder.therapistVerifiedAccount());

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> authenticationService.signIn(request))
                .extracting(PermissionException::getErrorCode)
                .isEqualTo(ErrorCode.AUTHENTICATION_FAILED);

        verifyNoInteractions(jwtAuthTokenProvider, uuidTokenProvider, uuidSupplier);
    }

    @DisplayName("""
            signIn
            SHOULD throw PermissionException with AUTHENTICATION_FAILED
            AND SHOULD NOT issue tokens
            WHEN the email does not exist
            """)
    @Test
    void signIn_error1() {
        PasswordSignInRequest request = new PasswordSignInRequest(DEFAULT_EMAIL, CORRECT_PASSWORD);

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new UsernameNotFoundException("Account not found"));

        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> authenticationService.signIn(request))
                .extracting(PermissionException::getErrorCode)
                .isEqualTo(ErrorCode.AUTHENTICATION_FAILED);

        verifyNoInteractions(accountService, jwtAuthTokenProvider, uuidTokenProvider, uuidSupplier);
    }

    @DisplayName("""
            signIn
            SHOULD throw PermissionException with EMAIL_NOT_VERIFIED
            AND SHOULD NOT issue tokens
            WHEN the current account is pending verification
            """)
    @Test
    void signIn_error2() {
        PasswordSignInRequest request = new PasswordSignInRequest(DEFAULT_EMAIL, CORRECT_PASSWORD);
        Account account = AccountTestBuilder.therapistUnverifiedAccount();
        TheraflowUser user = authenticatedUser();

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        mockLockedSignInAccount(account);

        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> authenticationService.signIn(request))
                .extracting(PermissionException::getErrorCode)
                .isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);

        assertThat(account.getRefreshTokens()).isEmpty();
        verifyNoInteractions(jwtAuthTokenProvider, uuidTokenProvider, uuidSupplier);
    }

    @DisplayName("""
            signIn
            SHOULD throw PermissionException with ACCOUNT_DISABLED
            AND SHOULD NOT issue tokens
            WHEN the current account is disabled
            """)
    @Test
    void signIn_error3() {
        PasswordSignInRequest request = new PasswordSignInRequest(DEFAULT_EMAIL, CORRECT_PASSWORD);
        Account account = new Account(AccountType.THERAPIST, AccountStatus.DISABLED);
        TheraflowUser user = authenticatedUser();

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        mockLockedSignInAccount(account);

        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> authenticationService.signIn(request))
                .extracting(PermissionException::getErrorCode)
                .isEqualTo(ErrorCode.ACCOUNT_DISABLED);

        assertThat(account.getRefreshTokens()).isEmpty();
        verifyNoInteractions(jwtAuthTokenProvider, uuidTokenProvider, uuidSupplier);
    }

    @DisplayName("""
            rotateTokens
            SHOULD revoke the presented refresh token and return a new token pair
            WHEN the refresh token is valid and the account is active
            """)
    @Test
    void rotateTokens_success() {
        String rawToken = "previous-refresh-token";
        Account account = AccountTestBuilder.therapistVerifiedAccount();
        ReflectionTestUtils.setField(account, "id", ACCOUNT_ID);
        account.setAuthIdentity(new AuthIdentity(AuthProvider.PASSWORD, DEFAULT_EMAIL, DEFAULT_EMAIL, "password-hash"));
        RefreshToken previousToken = new RefreshToken("previous-token-hash", clock.instant().plusSeconds(60));
        account.addRefreshToken(previousToken);
        RefreshToken replacementToken = new RefreshToken("replacement-token-hash", clock.instant().plusSeconds(3600));

        when(uuidTokenProvider.hashToken(rawToken)).thenReturn("previous-token-hash");
        mockLockedRefreshTokenLookup(previousToken);
        when(jwtAuthTokenProvider.generateAccessToken(any(TheraflowUser.class))).thenReturn("new-access-token");
        when(uuidSupplier.get()).thenReturn(RANDOM_UUID);
        when(uuidTokenProvider.buildRefreshToken(RANDOM_UUID.toString())).thenReturn(replacementToken);

        JwtPair response = authenticationService.rotateTokens(rawToken);

        assertThat(response).isEqualTo(new JwtPair("new-access-token", RANDOM_UUID.toString()));
        assertThat(previousToken.isRevoked()).isTrue();
        assertThat(replacementToken.isRevoked()).isFalse();
        assertThat(account.getRefreshTokens()).containsExactlyInAnyOrder(previousToken, replacementToken);
        assertThat(replacementToken.getAccount()).isSameAs(account);
        ArgumentCaptor<TheraflowUser> userCaptor = ArgumentCaptor.forClass(TheraflowUser.class);
        verify(jwtAuthTokenProvider).generateAccessToken(userCaptor.capture());
        assertThat(userCaptor.getValue().getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(userCaptor.getValue().getUsername()).isEqualTo(DEFAULT_EMAIL);
        assertThat(userCaptor.getValue().getType()).isEqualTo(AccountType.THERAPIST);
        var order = inOrder(accountService, refreshTokenRepository);
        order.verify(refreshTokenRepository).findAccountIdByTokenHash("previous-token-hash");
        order.verify(accountService).findByIdForUpdate(ACCOUNT_ID);
        order.verify(refreshTokenRepository).findByTokenHash("previous-token-hash");
    }

    @DisplayName("""
            rotateTokens
            SHOULD throw PermissionException with REFRESH_TOKEN_INVALID
            WHEN the token does not exist
            """)
    @Test
    void rotateTokens_error() {
        String rawToken = RANDOM_UUID.toString();

        when(uuidTokenProvider.hashToken(rawToken)).thenReturn("token-hash");
        when(refreshTokenRepository.findAccountIdByTokenHash("token-hash")).thenReturn(Optional.empty());

        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> authenticationService.rotateTokens(rawToken))
                .extracting(PermissionException::getErrorCode)
                .isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID);

        verifyNoInteractions(jwtAuthTokenProvider, uuidSupplier);
        verify(uuidTokenProvider, never()).buildRefreshToken(any());
    }

    @DisplayName("""
            rotateTokens
            SHOULD throw PermissionException with REFRESH_TOKEN_INVALID
            WHEN the token has already been revoked
            """)
    @Test
    void rotateTokens_error1() {
        String rawToken = RANDOM_UUID.toString();
        RefreshToken token = new RefreshToken("token-hash", clock.instant().plusSeconds(60));
        token.setRevoked(true);

        when(uuidTokenProvider.hashToken(rawToken)).thenReturn("token-hash");
        mockLockedRefreshTokenLookup(token);

        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> authenticationService.rotateTokens(rawToken))
                .extracting(PermissionException::getErrorCode)
                .isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID);

        verifyNoInteractions(jwtAuthTokenProvider, uuidSupplier);
        verify(uuidTokenProvider, never()).buildRefreshToken(any());
    }

    @DisplayName("""
            rotateTokens
            SHOULD throw PermissionException with REFRESH_TOKEN_INVALID
            WHEN the token is expired
            """)
    @Test
    void rotateTokens_error2() {
        String rawToken = RANDOM_UUID.toString();
        RefreshToken token = new RefreshToken("token-hash", clock.instant().minusSeconds(1));

        when(uuidTokenProvider.hashToken(rawToken)).thenReturn("token-hash");
        mockLockedRefreshTokenLookup(token);

        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> authenticationService.rotateTokens(rawToken))
                .extracting(PermissionException::getErrorCode)
                .isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID);

        assertThat(token.isRevoked()).isFalse();
        verifyNoInteractions(jwtAuthTokenProvider, uuidSupplier);
        verify(uuidTokenProvider, never()).buildRefreshToken(any());
    }

    @DisplayName("""
            rotateTokens
            SHOULD throw PermissionException with REFRESH_TOKEN_INVALID
            WHEN the token expires exactly now
            """)
    @Test
    void rotateTokens_error3() {
        String rawToken = RANDOM_UUID.toString();
        RefreshToken token = new RefreshToken("token-hash", clock.instant());

        when(uuidTokenProvider.hashToken(rawToken)).thenReturn("token-hash");
        mockLockedRefreshTokenLookup(token);

        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> authenticationService.rotateTokens(rawToken))
                .extracting(PermissionException::getErrorCode)
                .isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID);

        assertThat(token.isRevoked()).isFalse();
        verifyNoInteractions(jwtAuthTokenProvider, uuidSupplier);
        verify(uuidTokenProvider, never()).buildRefreshToken(any());
    }

    @DisplayName("""
            rotateTokens
            SHOULD throw PermissionException with ACCOUNT_DISABLED
            AND SHOULD NOT rotate the token
            WHEN the account has been disabled
            """)
    @Test
    void rotateTokens_error4() {
        String rawToken = RANDOM_UUID.toString();
        Account account = new Account(AccountType.THERAPIST, AccountStatus.DISABLED);
        RefreshToken token = new RefreshToken("token-hash", clock.instant().plusSeconds(60));
        account.addRefreshToken(token);

        when(uuidTokenProvider.hashToken(rawToken)).thenReturn("token-hash");
        mockLockedRefreshTokenLookup(token);

        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> authenticationService.rotateTokens(rawToken))
                .extracting(PermissionException::getErrorCode)
                .isEqualTo(ErrorCode.ACCOUNT_DISABLED);

        assertThat(token.isRevoked()).isFalse();
        assertThat(account.getRefreshTokens()).containsExactly(token);
        verifyNoInteractions(jwtAuthTokenProvider, uuidSupplier);
        verify(uuidTokenProvider, never()).buildRefreshToken(any());
    }

    @DisplayName("""
            rotateTokens
            SHOULD throw PermissionException with EMAIL_NOT_VERIFIED
            AND SHOULD NOT rotate the token
            WHEN the account is pending verification
            """)
    @Test
    void rotateTokens_error5() {
        String rawToken = RANDOM_UUID.toString();
        Account account = AccountTestBuilder.therapistUnverifiedAccount();
        RefreshToken token = new RefreshToken("token-hash", clock.instant().plusSeconds(60));
        account.addRefreshToken(token);

        when(uuidTokenProvider.hashToken(rawToken)).thenReturn("token-hash");
        mockLockedRefreshTokenLookup(token);

        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> authenticationService.rotateTokens(rawToken))
                .extracting(PermissionException::getErrorCode)
                .isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);

        assertThat(token.isRevoked()).isFalse();
        assertThat(account.getRefreshTokens()).containsExactly(token);
        verifyNoInteractions(jwtAuthTokenProvider, uuidSupplier);
        verify(uuidTokenProvider, never()).buildRefreshToken(any());
    }

    private void mockLockedSignInAccount(Account account) {
        ReflectionTestUtils.setField(account, "id", ACCOUNT_ID);
        when(authIdentityRepository.findAccountIdByProviderAndProviderUserId(AuthProvider.PASSWORD, DEFAULT_EMAIL))
                .thenReturn(Optional.of(ACCOUNT_ID));
        when(accountService.findByIdForUpdate(ACCOUNT_ID)).thenReturn(account);
    }

    private void mockLockedRefreshTokenLookup(RefreshToken token) {
        Account account = token.getAccount();
        if (account == null) {
            account = AccountTestBuilder.therapistVerifiedAccount();
            account.addRefreshToken(token);
        }
        ReflectionTestUtils.setField(account, "id", ACCOUNT_ID);
        when(refreshTokenRepository.findAccountIdByTokenHash(token.getTokenHash())).thenReturn(Optional.of(ACCOUNT_ID));
        when(accountService.findByIdForUpdate(ACCOUNT_ID)).thenReturn(account);
        when(refreshTokenRepository.findByTokenHash(token.getTokenHash())).thenReturn(Optional.of(token));
    }

    private TheraflowUser authenticatedUser() {
        return new TheraflowUser(ACCOUNT_ID, DEFAULT_EMAIL, null, AccountType.THERAPIST, AccountStatus.ACTIVE);
    }
}
