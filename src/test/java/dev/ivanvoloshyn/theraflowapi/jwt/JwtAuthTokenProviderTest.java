package dev.ivanvoloshyn.theraflowapi.jwt;

import dev.ivanvoloshyn.theraflowapi.account.dto.AccountStatus;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountType;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.PermissionException;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import dev.ivanvoloshyn.theraflowapi.security.model.TheraflowUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class JwtAuthTokenProviderTest {
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Duration EXPIRATION = Duration.ofMinutes(15);
    private static final String SECRET = Base64.getEncoder().encodeToString(
            "test-access-secret-used-only-in-tests-0123456789012345678901234567"
                    .getBytes(StandardCharsets.UTF_8));
    private static final String OTHER_SECRET = Base64.getEncoder().encodeToString(
            "test-email-secret-used-only-in-tests-01234567890123456789012345678"
                    .getBytes(StandardCharsets.UTF_8));
    private static final UUID ACCOUNT_ID = UUID.fromString("3a49a855-38b6-4d05-82f2-8ff21ca838a5");
    private static final String EMAIL = "therapist@example.com";

    private final JwtProvider jwtProvider = new JwtProvider(CLOCK);
    private JwtAuthTokenProvider jwtAuthTokenProvider;

    @BeforeEach
    void setUp() {
        jwtAuthTokenProvider = authTokenProvider(jwtProvider);
    }

    @DisplayName("""
            generateAccessToken
            SHOULD create signed access token
            WITH account identity AND configured expiration
            WITHOUT password credentials
            """)
    @Test
    void generateAccessToken_success() {
        TheraflowUser user = new TheraflowUser(
                ACCOUNT_ID, EMAIL, "password-hash", AccountType.THERAPIST, AccountStatus.ACTIVE);

        String token = jwtAuthTokenProvider.generateAccessToken(user);
        Claims claims = jwtProvider.extractAllClaims(token, SECRET);

        assertThat(claims.getSubject()).isEqualTo(EMAIL);
        assertThat(claims.get("accountId", String.class)).isEqualTo(ACCOUNT_ID.toString());
        assertThat(claims.get("account_type", String.class)).isEqualTo(AccountType.THERAPIST.name());
        assertThat(claims.get("token_type", String.class)).isEqualTo("access");
        assertThat(claims.getIssuedAt().toInstant()).isEqualTo(NOW);
        assertThat(claims.getExpiration().toInstant()).isEqualTo(NOW.plus(EXPIRATION));
        assertThat(claims).doesNotContainKeys("password", "passwordHash");
    }

    @DisplayName("""
            extractUserDetails
            SHOULD return principal without credentials
            WITH account identity AND authority
            WHEN access token is valid
            """)
    @Test
    void extractUserDetails_success() {
        TheraflowUser user = new TheraflowUser(
                ACCOUNT_ID, EMAIL, "password-hash", AccountType.GUARDIAN, AccountStatus.ACTIVE);
        String token = jwtAuthTokenProvider.generateAccessToken(user);

        TheraflowUser actual = jwtAuthTokenProvider.extractUserDetails(token);

        assertThat(actual.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(actual.getUsername()).isEqualTo(EMAIL);
        assertThat(actual.getType()).isEqualTo(AccountType.GUARDIAN);
        assertThat(actual.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(actual.getPassword()).isNull();
        assertThat(actual.getAuthorities()).extracting("authority").containsExactly("ROLE_GUARDIAN");
    }

    @DisplayName("""
            extractUserDetails
            SHOULD throw PermissionException
            WITH TOKEN_EXPIRED
            WHEN access token is expired
            """)
    @Test
    void extractUserDetails_error_expiredToken() {
        String token = validTokenBuilder().compact();
        Clock expiredClock = Clock.fixed(NOW.plus(EXPIRATION).plusSeconds(1), ZoneOffset.UTC);
        JwtAuthTokenProvider expiredTokenProvider = authTokenProvider(new JwtProvider(expiredClock));

        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> expiredTokenProvider.extractUserDetails(token))
                .extracting(PermissionException::getErrorCode)
                .isEqualTo(ErrorCode.TOKEN_EXPIRED);
    }

    @DisplayName("""
            extractUserDetails
            SHOULD throw PermissionException
            WITH TOKEN_EXPIRED
            WHEN access token expires exactly now
            """)
    @Test
    void extractUserDetails_error_expirationBoundary() {
        String token = validTokenBuilder().compact();
        Clock expirationClock = Clock.fixed(NOW.plus(EXPIRATION), ZoneOffset.UTC);
        JwtAuthTokenProvider expiredTokenProvider = authTokenProvider(new JwtProvider(expirationClock));

        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> expiredTokenProvider.extractUserDetails(token))
                .extracting(PermissionException::getErrorCode)
                .isEqualTo(ErrorCode.TOKEN_EXPIRED);
    }

    @DisplayName("""
            extractUserDetails
            SHOULD accept the access token
            WHEN current time is just before expiration
            """)
    @Test
    void extractUserDetails_success_beforeExpiration() {
        String token = validTokenBuilder().compact();
        Clock beforeExpiration = Clock.fixed(NOW.plus(EXPIRATION).minusMillis(1), ZoneOffset.UTC);
        JwtAuthTokenProvider provider = authTokenProvider(new JwtProvider(beforeExpiration));

        assertThat(provider.extractUserDetails(token).getAccountId()).isEqualTo(ACCOUNT_ID);
    }

    @DisplayName("""
            extractUserDetails
            SHOULD throw PermissionException
            WITH TOKEN_INVALID
            WHEN token is malformed or missing
            """)
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "invalid-token", "header.payload.signature"})
    void extractUserDetails_error_malformedToken(String token) {
        assertInvalidToken(token);
    }

    @DisplayName("""
            extractUserDetails
            SHOULD throw PermissionException
            WITH TOKEN_INVALID
            WHEN token signature is tampered
            """)
    @Test
    void extractUserDetails_error_tamperedSignature() {
        String token = validTokenBuilder().compact();
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tamperedToken = token.substring(0, signatureStart)
                + replacement + token.substring(signatureStart + 1);

        assertInvalidToken(tamperedToken);
    }

    @DisplayName("""
            extractUserDetails
            SHOULD throw PermissionException
            WITH TOKEN_INVALID
            WHEN token was signed with a different secret
            """)
    @Test
    void extractUserDetails_error_wrongSecret() {
        JwtAuthTokenProvider otherProvider = authTokenProvider(jwtProvider);
        ReflectionTestUtils.setField(otherProvider, "secret", OTHER_SECRET);
        String token = otherProvider.generateAccessToken(new TheraflowUser(
                ACCOUNT_ID, EMAIL, null, AccountType.THERAPIST, AccountStatus.ACTIVE));

        assertInvalidToken(token);
    }

    @DisplayName("""
            extractUserDetails
            SHOULD throw PermissionException
            WITH TOKEN_INVALID
            WHEN a required claim is missing
            """)
    @ParameterizedTest
    @ValueSource(strings = {"sub", "accountId", "account_type", "token_type", "exp"})
    void extractUserDetails_error_missingClaim(String claim) {
        String token = validTokenBuilder().claim(claim, null).compact();

        assertInvalidToken(token);
    }

    @DisplayName("""
            extractUserDetails
            SHOULD throw PermissionException
            WITH TOKEN_INVALID
            WHEN an identity or token purpose claim is invalid
            """)
    @ParameterizedTest
    @MethodSource("invalidClaims")
    void extractUserDetails_error_invalidClaim(String claim, Object value) {
        String token = validTokenBuilder().claim(claim, value).compact();

        assertInvalidToken(token);
    }

    @DisplayName("""
            extractUserDetails
            SHOULD throw PermissionException
            WITH TOKEN_INVALID
            WHEN email verification token uses the same signing secret
            """)
    @Test
    void extractUserDetails_error_emailVerificationToken() {
        EmailVerificationTokenProvider emailTokenProvider = new EmailVerificationTokenProvider(jwtProvider);
        ReflectionTestUtils.setField(emailTokenProvider, "secret", SECRET);
        ReflectionTestUtils.setField(emailTokenProvider, "expiration", EXPIRATION);
        String token = emailTokenProvider.generateToken(EMAIL);

        assertInvalidToken(token);
    }

    private static Stream<Arguments> invalidClaims() {
        return Stream.of(
                Arguments.of("sub", " "),
                Arguments.of("accountId", "not-a-uuid"),
                Arguments.of("accountId", 42),
                Arguments.of("account_type", "ADMIN"),
                Arguments.of("account_type", 42),
                Arguments.of("token_type", "email"),
                Arguments.of("token_type", 42)
        );
    }

    private JwtBuilder validTokenBuilder() {
        return jwtProvider.tokenBuilder(EXPIRATION, SECRET)
                .subject(EMAIL)
                .claim("accountId", ACCOUNT_ID.toString())
                .claim("account_type", AccountType.THERAPIST.name())
                .claim("token_type", "access");
    }

    private JwtAuthTokenProvider authTokenProvider(JwtProvider provider) {
        JwtAuthTokenProvider result = new JwtAuthTokenProvider(provider);
        ReflectionTestUtils.setField(result, "secret", SECRET);
        ReflectionTestUtils.setField(result, "expiration", EXPIRATION);
        return result;
    }

    private void assertInvalidToken(String token) {
        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> jwtAuthTokenProvider.extractUserDetails(token))
                .extracting(PermissionException::getErrorCode)
                .isEqualTo(ErrorCode.TOKEN_INVALID);
    }
}
