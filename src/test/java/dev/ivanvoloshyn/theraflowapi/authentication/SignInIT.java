package dev.ivanvoloshyn.theraflowapi.authentication;

import dev.ivanvoloshyn.theraflowapi.TestContainerConfiguration;
import dev.ivanvoloshyn.theraflowapi.account.AccountRepository;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountStatus;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountType;
import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.ConfirmPasswordResetRequest;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.JwtPair;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.PasswordSignInRequest;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.TokenRotateRequest;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthIdentity;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthProvider;
import dev.ivanvoloshyn.theraflowapi.authentication.model.PasswordResetToken;
import dev.ivanvoloshyn.theraflowapi.authentication.model.RefreshToken;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorResponse;
import dev.ivanvoloshyn.theraflowapi.jwt.EmailVerificationTokenProvider;
import dev.ivanvoloshyn.theraflowapi.jwt.JwtProvider;
import dev.ivanvoloshyn.theraflowapi.security.UUIDTokenProvider;
import dev.ivanvoloshyn.theraflowapi.security.model.TheraflowUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.CONFIRM_PASSWORD_RESET_ENDPOINT;
import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.CORRECT_PASSWORD;
import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.DEFAULT_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import({TestContainerConfiguration.class, SignInIT.ProtectedAccountController.class})
class SignInIT {

    private static final String SIGN_IN_ENDPOINT = "/api/v1/auth/password/sign-in";
    private static final String REFRESH_ENDPOINT = "/api/v1/auth/refresh";
    private static final String ACCOUNT_ENDPOINT = "/api/v1/test/account";
    private static final String THERAPIST_ENDPOINT = "/api/v1/test/therapist";

    @Autowired
    private RestTestClient restTestClient;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UUIDTokenProvider uuidTokenProvider;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private EmailVerificationTokenProvider emailVerificationTokenProvider;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private PasswordAuthenticationService passwordAuthenticationService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Value("${app.security.jwt.access.secret}")
    private String accessSecret;

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            refreshTokenRepository.deleteAllInBatch();
            accountRepository.deleteAll();
        });
    }

    @DisplayName("""
            signIn
            SHOULD return tokens AND persist only refresh token hash
            AND authenticate protected requests WITH Account identity and role
            WHEN credentials match an active Account
            """)
    @Test
    void signIn_success() {
        UUID accountId = persistAccount(AccountType.THERAPIST, AccountStatus.ACTIVE);

        JwtPair tokens = signIn(DEFAULT_EMAIL, CORRECT_PASSWORD)
                .expectStatus().isOk()
                .returnResult(JwtPair.class).getResponseBody();

        assertThat(tokens).isNotNull();
        assertThat(tokens.access()).isNotBlank();
        assertThat(tokens.refresh()).isNotBlank().isNotEqualTo(tokens.access());
        transactionTemplate.executeWithoutResult(status -> {
            assertThat(refreshTokenRepository.findAll()).singleElement().satisfies(token -> {
                assertThat(token.getAccount().getId()).isEqualTo(accountId);
                assertThat(token.getTokenHash()).isEqualTo(uuidTokenProvider.hashToken(tokens.refresh()))
                        .isNotEqualTo(tokens.refresh());
                assertThat(token.getExpiresAt()).isAfter(Instant.now());
                assertThat(token.isRevoked()).isFalse();
            });
        });

        protectedRequest(ACCOUNT_ENDPOINT, tokens.access())
                .expectStatus().isOk()
                .expectBody(AccountProbe.class)
                .value(account -> {
                    assertThat(account.accountId()).isEqualTo(accountId);
                    assertThat(account.authorities()).containsExactly("ROLE_THERAPIST");
                    assertThat(account.credentialsErased()).isTrue();
                });
        protectedRequest(THERAPIST_ENDPOINT, tokens.access()).expectStatus().isOk();
    }

    @DisplayName("""
            signIn
            SHOULD normalize email before authentication
            WHEN email contains uppercase characters
            """)
    @Test
    void signIn_uppercaseEmail() {
        persistAccount(AccountType.THERAPIST, AccountStatus.ACTIVE);

        signIn("TEST@GMAIL.COM", CORRECT_PASSWORD).expectStatus().isOk();
    }

    @DisplayName("""
            signIn
            SHOULD return UNAUTHORIZED WITH AUTHENTICATION_FAILED
            AND create no refresh tokens
            WHEN credentials are invalid regardless of Account status
            """)
    @ParameterizedTest
    @ValueSource(strings = {"UNKNOWN", "ACTIVE", "PENDING_VERIFICATION", "DISABLED"})
    void signIn_invalidCredentials(String accountState) {
        if (!accountState.equals("UNKNOWN")) {
            persistAccount(AccountType.THERAPIST, AccountStatus.valueOf(accountState));
        }

        expectError(signIn(DEFAULT_EMAIL, "WrongPassword!123"), ErrorCode.AUTHENTICATION_FAILED);

        assertThat(refreshTokenRepository.count()).isZero();
    }

    @DisplayName("""
            signIn
            SHOULD reject inactive Account WITH matching error code
            AND create no refresh tokens
            WHEN the password is correct
            """)
    @ParameterizedTest
    @CsvSource({"PENDING_VERIFICATION, EMAIL_NOT_VERIFIED", "DISABLED, ACCOUNT_DISABLED"})
    void signIn_inactiveAccount(AccountStatus accountStatus, ErrorCode errorCode) {
        persistAccount(AccountType.THERAPIST, accountStatus);

        expectError(signIn(DEFAULT_EMAIL, CORRECT_PASSWORD), errorCode);

        assertThat(refreshTokenRepository.count()).isZero();
    }

    @DisplayName("""
            protectedRequest
            SHOULD return UNAUTHORIZED WITH matching error code
            WHEN access token is absent, malformed, expired or issued for email verification
            """)
    @ParameterizedTest
    @CsvSource({
            "ABSENT, AUTHENTICATION_REQUIRED",
            "MALFORMED, TOKEN_INVALID",
            "EXPIRED, TOKEN_EXPIRED",
            "EMAIL_VERIFICATION, TOKEN_INVALID"
    })
    void protectedRequest_invalidToken(String tokenType, ErrorCode errorCode) {
        UUID accountId = persistAccount(AccountType.THERAPIST, AccountStatus.ACTIVE);
        String token = switch (tokenType) {
            case "ABSENT" -> null;
            case "MALFORMED" -> "not-a-jwt";
            case "EXPIRED" -> jwtProvider.tokenBuilder(Duration.ofMinutes(-1), accessSecret)
                    .subject(accountId.toString()).compact();
            case "EMAIL_VERIFICATION" -> emailVerificationTokenProvider.generateToken(DEFAULT_EMAIL);
            default -> throw new IllegalArgumentException(tokenType);
        };

        expectError(protectedRequest(ACCOUNT_ENDPOINT, token), errorCode);
    }

    @DisplayName("""
            protectedRequest
            SHOULD return FORBIDDEN WITH ACCESS_DENIED
            WHEN authenticated Account does not have the required role
            """)
    @Test
    void protectedRequest_wrongRole() {
        persistAccount(AccountType.GUARDIAN, AccountStatus.ACTIVE);
        JwtPair tokens = signInSuccessfully();

        protectedRequest(ACCOUNT_ENDPOINT, tokens.access()).expectStatus().isOk();
        expectError(protectedRequest(THERAPIST_ENDPOINT, tokens.access()), ErrorCode.ACCESS_DENIED);
    }

    @DisplayName("""
            protectedRequest
            SHOULD return FORBIDDEN WITH ACCOUNT_DISABLED
            WHEN Account was disabled after access token issuance
            """)
    @Test
    void protectedRequest_accountDisabled() {
        UUID accountId = persistAccount(AccountType.THERAPIST, AccountStatus.ACTIVE);
        JwtPair tokens = signInSuccessfully();
        disableAccount(accountId);

        expectError(protectedRequest(ACCOUNT_ENDPOINT, tokens.access()), ErrorCode.ACCOUNT_DISABLED);
    }

    @DisplayName("""
            refresh
            SHOULD rotate tokens AND reject reuse of the previous refresh token
            WHEN refresh token is valid
            """)
    @Test
    void refresh_success() {
        UUID accountId = persistAccount(AccountType.THERAPIST, AccountStatus.ACTIVE);
        JwtPair original = signInSuccessfully();

        JwtPair rotated = refresh(original.refresh())
                .expectStatus().isOk()
                .returnResult(JwtPair.class).getResponseBody();

        assertThat(rotated).isNotNull();
        assertThat(rotated.access()).isNotBlank();
        assertThat(rotated.refresh()).isNotBlank().isNotEqualTo(original.refresh());
        transactionTemplate.executeWithoutResult(status -> {
            assertThat(refreshTokenRepository.findByTokenHash(uuidTokenProvider.hashToken(original.refresh())))
                    .hasValueSatisfying(token -> assertThat(token.isRevoked()).isTrue());
            assertThat(refreshTokenRepository.findByTokenHash(uuidTokenProvider.hashToken(rotated.refresh())))
                    .hasValueSatisfying(token -> {
                        assertThat(token.getAccount().getId()).isEqualTo(accountId);
                        assertThat(token.isRevoked()).isFalse();
                    });
        });
        protectedRequest(ACCOUNT_ENDPOINT, rotated.access()).expectStatus().isOk();

        expectError(refresh(original.refresh()), ErrorCode.REFRESH_TOKEN_INVALID);

        assertThat(refreshTokenRepository.count()).isEqualTo(2);
    }

    @DisplayName("""
            refresh
            SHOULD accept exactly one request AND reject the other WITH REFRESH_TOKEN_INVALID
            AND persist exactly one replacement token
            WHEN two requests concurrently use the same refresh token
            """)
    @Test
    void refresh_concurrentRequests() throws Exception {
        persistAccount(AccountType.THERAPIST, AccountStatus.ACTIVE);
        JwtPair original = signInSuccessfully();
        CyclicBarrier start = new CyclicBarrier(3);
        List<RestTestClient.ResponseSpec> responses;

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return refresh(original.refresh());
            });
            var second = executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return refresh(original.refresh());
            });
            start.await(5, TimeUnit.SECONDS);
            responses = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
        }

        List<Integer> statuses = new ArrayList<>();
        List<JwtPair> replacements = new ArrayList<>();
        for (RestTestClient.ResponseSpec response : responses) {
            AtomicInteger status = new AtomicInteger();
            response.expectStatus().value(status::set);
            statuses.add(status.get());
            if (status.get() == 200) {
                replacements.add(response.returnResult(JwtPair.class).getResponseBody());
            } else {
                expectError(response, ErrorCode.REFRESH_TOKEN_INVALID);
            }
        }

        assertThat(statuses).containsExactlyInAnyOrder(200, 401);
        assertThat(replacements).singleElement().isNotNull();
        String replacementHash = uuidTokenProvider.hashToken(replacements.getFirst().refresh());
        transactionTemplate.executeWithoutResult(status -> {
            List<RefreshToken> tokens = refreshTokenRepository.findAll();
            assertThat(tokens).hasSize(2);
            assertThat(tokens).filteredOn(RefreshToken::isRevoked).singleElement()
                    .extracting(RefreshToken::getTokenHash)
                    .isEqualTo(uuidTokenProvider.hashToken(original.refresh()));
            assertThat(tokens).filteredOn(token -> !token.isRevoked()).singleElement()
                    .extracting(RefreshToken::getTokenHash).isEqualTo(replacementHash);
        });
    }

    @DisplayName("""
            confirmPasswordReset
            SHOULD revoke existing refresh tokens AND accept only the new password
            WHEN password reset succeeds
            """)
    @Test
    void confirmPasswordReset_revokesRefreshTokens() {
        UUID accountId = persistAccount(AccountType.THERAPIST, AccountStatus.ACTIVE);
        JwtPair firstSession = signInSuccessfully();
        JwtPair secondSession = signInSuccessfully();
        String rawResetToken = UUID.randomUUID().toString();
        String newPassword = "NewPassword!456";
        transactionTemplate.executeWithoutResult(status -> {
            Account account = accountRepository.findById(accountId).orElseThrow();
            account.addPasswordResetToken(new PasswordResetToken(uuidTokenProvider.hashToken(rawResetToken),
                    Instant.now().plusSeconds(3600)));
        });

        restTestClient.post()
                .uri(CONFIRM_PASSWORD_RESET_ENDPOINT)
                .body(new ConfirmPasswordResetRequest(rawResetToken, newPassword, newPassword))
                .exchange()
                .expectStatus().isOk();

        assertThat(refreshTokenRepository.findAll()).hasSize(2)
                .allSatisfy(token -> assertThat(token.isRevoked()).isTrue());
        expectError(refresh(firstSession.refresh()), ErrorCode.REFRESH_TOKEN_INVALID);
        expectError(refresh(secondSession.refresh()), ErrorCode.REFRESH_TOKEN_INVALID);
        expectError(signIn(DEFAULT_EMAIL, CORRECT_PASSWORD), ErrorCode.AUTHENTICATION_FAILED);

        JwtPair newSession = signIn(DEFAULT_EMAIL, newPassword)
                .expectStatus().isOk()
                .returnResult(JwtPair.class).getResponseBody();

        assertThat(newSession).isNotNull();
        protectedRequest(ACCOUNT_ENDPOINT, newSession.access()).expectStatus().isOk();
    }

    @DisplayName("""
            refresh
            SHOULD reject token WITH REFRESH_TOKEN_INVALID AND create no replacement
            WHEN password reset commits while rotation waits for the Account lock
            """)
    @Test
    void refresh_concurrentPasswordReset() throws Exception {
        UUID accountId = persistAccount(AccountType.THERAPIST, AccountStatus.ACTIVE);
        JwtPair session = signInSuccessfully();
        String rawResetToken = UUID.randomUUID().toString();
        String newPassword = "NewPassword!456";
        transactionTemplate.executeWithoutResult(status -> {
            Account account = accountRepository.findById(accountId).orElseThrow();
            account.addPasswordResetToken(new PasswordResetToken(uuidTokenProvider.hashToken(rawResetToken),
                    Instant.now().plusSeconds(3600)));
        });
        CountDownLatch startRefresh = new CountDownLatch(1);

        try (var executor = Executors.newSingleThreadExecutor()) {
            var refreshAttempt = executor.submit(() -> {
                assertThat(startRefresh.await(5, TimeUnit.SECONDS)).isTrue();
                return refresh(session.refresh());
            });

            transactionTemplate.executeWithoutResult(status -> {
                accountRepository.findByIdForUpdate(accountId).orElseThrow();
                Integer blockingPid = jdbcTemplate.queryForObject("SELECT pg_backend_pid()", Integer.class);
                startRefresh.countDown();

                await().alias("refresh waiting for the locked Account")
                        .atMost(Duration.ofSeconds(5))
                        .until(() -> Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                                SELECT EXISTS (
                                    SELECT 1 FROM pg_stat_activity
                                    WHERE ? = ANY(pg_blocking_pids(pid))
                                      AND query ILIKE '%accounts%'
                                )
                                """, Boolean.class, blockingPid)));

                passwordAuthenticationService.confirmPasswordReset(
                        new ConfirmPasswordResetRequest(rawResetToken, newPassword, newPassword));
            });

            expectError(refreshAttempt.get(15, TimeUnit.SECONDS), ErrorCode.REFRESH_TOKEN_INVALID);
        }

        assertThat(refreshTokenRepository.findAll()).singleElement().satisfies(token -> {
            assertThat(token.getTokenHash()).isEqualTo(uuidTokenProvider.hashToken(session.refresh()));
            assertThat(token.isRevoked()).isTrue();
        });
    }

    @DisplayName("""
            signIn
            SHOULD authenticate only the new password AND issue tokens only for valid credentials
            WHEN password reset commits while sign-in waits for the Account lock
            """)
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void signIn_concurrentPasswordReset(boolean useNewPassword) throws Exception {
        UUID accountId = persistAccount(AccountType.THERAPIST, AccountStatus.ACTIVE);
        String rawResetToken = UUID.randomUUID().toString();
        String newPassword = "NewPassword!456";
        String submittedPassword = useNewPassword ? newPassword : CORRECT_PASSWORD;
        transactionTemplate.executeWithoutResult(status -> {
            Account account = accountRepository.findById(accountId).orElseThrow();
            account.addPasswordResetToken(new PasswordResetToken(uuidTokenProvider.hashToken(rawResetToken),
                    Instant.now().plusSeconds(3600)));
        });
        CountDownLatch startSignIn = new CountDownLatch(1);
        RestTestClient.ResponseSpec response;

        try (var executor = Executors.newSingleThreadExecutor()) {
            var signInAttempt = executor.submit(() -> {
                assertThat(startSignIn.await(5, TimeUnit.SECONDS)).isTrue();
                return signIn(DEFAULT_EMAIL, submittedPassword);
            });

            transactionTemplate.executeWithoutResult(status -> {
                accountRepository.findByIdForUpdate(accountId).orElseThrow();
                Integer blockingPid = jdbcTemplate.queryForObject("SELECT pg_backend_pid()", Integer.class);
                startSignIn.countDown();

                await().alias("sign-in waiting for the locked Account before checking credentials")
                        .atMost(Duration.ofSeconds(5))
                        .until(() -> Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                                SELECT EXISTS (
                                    SELECT 1 FROM pg_stat_activity
                                    WHERE ? = ANY(pg_blocking_pids(pid))
                                      AND query ILIKE '%accounts%'
                                )
                                """, Boolean.class, blockingPid)));

                passwordAuthenticationService.confirmPasswordReset(
                        new ConfirmPasswordResetRequest(rawResetToken, newPassword, newPassword));
            });

            response = signInAttempt.get(15, TimeUnit.SECONDS);
        }

        if (useNewPassword) {
            JwtPair tokens = response.expectStatus().isOk()
                    .returnResult(JwtPair.class).getResponseBody();

            assertThat(tokens).isNotNull();
            transactionTemplate.executeWithoutResult(status -> {
                assertThat(refreshTokenRepository.findAll()).singleElement().satisfies(token -> {
                    assertThat(token.getAccount().getId()).isEqualTo(accountId);
                    assertThat(token.getTokenHash()).isEqualTo(uuidTokenProvider.hashToken(tokens.refresh()))
                            .isNotEqualTo(tokens.refresh());
                    assertThat(token.isRevoked()).isFalse();
                });
            });
            protectedRequest(ACCOUNT_ENDPOINT, tokens.access()).expectStatus().isOk();
        } else {
            expectError(response, ErrorCode.AUTHENTICATION_FAILED);
            assertThat(refreshTokenRepository.count()).isZero();
        }
    }

    @DisplayName("""
            refresh
            SHOULD return UNAUTHORIZED WITH REFRESH_TOKEN_INVALID
            WHEN refresh token is unknown, expired or revoked
            """)
    @ParameterizedTest
    @ValueSource(strings = {"UNKNOWN", "EXPIRED", "REVOKED"})
    void refresh_invalidToken(String tokenState) {
        UUID accountId = persistAccount(AccountType.THERAPIST, AccountStatus.ACTIVE);
        String rawToken = UUID.randomUUID().toString();
        if (!tokenState.equals("UNKNOWN")) {
            transactionTemplate.executeWithoutResult(status -> {
                Account account = accountRepository.findById(accountId).orElseThrow();
                Instant expiresAt = tokenState.equals("EXPIRED")
                        ? Instant.now().minusSeconds(60) : Instant.now().plusSeconds(3600);
                RefreshToken token = new RefreshToken(uuidTokenProvider.hashToken(rawToken), expiresAt);
                token.setRevoked(tokenState.equals("REVOKED"));
                account.addRefreshToken(token);
            });
        }

        expectError(refresh(rawToken), ErrorCode.REFRESH_TOKEN_INVALID);

        assertThat(refreshTokenRepository.count()).isEqualTo(tokenState.equals("UNKNOWN") ? 0 : 1);
    }

    @DisplayName("""
            refresh
            SHOULD return FORBIDDEN WITH ACCOUNT_DISABLED
            WHEN Account was disabled after refresh token issuance
            """)
    @Test
    void refresh_accountDisabled() {
        UUID accountId = persistAccount(AccountType.THERAPIST, AccountStatus.ACTIVE);
        JwtPair tokens = signInSuccessfully();
        disableAccount(accountId);

        expectError(refresh(tokens.refresh()), ErrorCode.ACCOUNT_DISABLED);

        assertThat(refreshTokenRepository.count()).isOne();
    }

    @DisplayName("""
            publicAuthenticationRequest
            SHOULD issue tokens AND ignore an expired bearer header
            WHEN valid credentials or refresh token are submitted
            """)
    @ParameterizedTest
    @ValueSource(strings = {SIGN_IN_ENDPOINT, REFRESH_ENDPOINT})
    void publicAuthenticationRequest_expiredBearer(String endpoint) {
        UUID accountId = persistAccount(AccountType.THERAPIST, AccountStatus.ACTIVE);
        JwtPair original = signInSuccessfully();
        String expiredAccess = jwtProvider.tokenBuilder(Duration.ofMinutes(-1), accessSecret)
                .subject(DEFAULT_EMAIL)
                .claim("accountId", accountId.toString())
                .claim("account_type", AccountType.THERAPIST.name())
                .claim("token_type", "access")
                .compact();
        Object request = endpoint.equals(SIGN_IN_ENDPOINT)
                ? new PasswordSignInRequest(DEFAULT_EMAIL, CORRECT_PASSWORD)
                : new TokenRotateRequest(original.refresh());

        JwtPair tokens = restTestClient.post().uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredAccess)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .returnResult(JwtPair.class).getResponseBody();

        assertThat(tokens).isNotNull();
        protectedRequest(ACCOUNT_ENDPOINT, tokens.access()).expectStatus().isOk();
    }

    private UUID persistAccount(AccountType type, AccountStatus status) {
        return transactionTemplate.execute(transactionStatus -> {
            Account account = new Account(type, status);
            account.setAuthIdentity(new AuthIdentity(AuthProvider.PASSWORD, DEFAULT_EMAIL,
                    DEFAULT_EMAIL, passwordEncoder.encode(CORRECT_PASSWORD)));
            return accountRepository.saveAndFlush(account).getId();
        });
    }

    private void disableAccount(UUID accountId) {
        transactionTemplate.executeWithoutResult(status ->
                accountRepository.findById(accountId).orElseThrow().setStatus(AccountStatus.DISABLED));
    }

    private JwtPair signInSuccessfully() {
        JwtPair tokens = signIn(DEFAULT_EMAIL, CORRECT_PASSWORD)
                .expectStatus().isOk()
                .returnResult(JwtPair.class).getResponseBody();
        assertThat(tokens).isNotNull();
        return tokens;
    }

    private RestTestClient.ResponseSpec signIn(String email, String password) {
        return restTestClient.post()
                .uri(SIGN_IN_ENDPOINT)
                .body(new PasswordSignInRequest(email, password))
                .exchange();
    }

    private RestTestClient.ResponseSpec refresh(String refreshToken) {
        return restTestClient.post()
                .uri(REFRESH_ENDPOINT)
                .body(new TokenRotateRequest(refreshToken))
                .exchange();
    }

    private RestTestClient.ResponseSpec protectedRequest(String endpoint, String accessToken) {
        var request = restTestClient.get().uri(endpoint);
        if (accessToken != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        return request.exchange();
    }

    private void expectError(RestTestClient.ResponseSpec response, ErrorCode errorCode) {
        response.expectStatus().isEqualTo(errorCode.getHttpStatus())
                .expectBody(ErrorResponse.class)
                .value(error -> {
                    assertThat(error.code()).isEqualTo(errorCode);
                    assertThat(error.status()).isEqualTo(errorCode.getHttpStatus().value());
                });
    }

    record AccountProbe(UUID accountId, List<String> authorities, boolean credentialsErased) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    @RestController
    static class ProtectedAccountController {

        @GetMapping(ACCOUNT_ENDPOINT)
        AccountProbe account(@AuthenticationPrincipal TheraflowUser user) {
            return new AccountProbe(user.getAccountId(), user.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).toList(), user.getPassword() == null);
        }

        @GetMapping(THERAPIST_ENDPOINT)
        @PreAuthorize("hasRole('THERAPIST')")
        AccountProbe therapist(@AuthenticationPrincipal TheraflowUser user) {
            return account(user);
        }
    }
}
