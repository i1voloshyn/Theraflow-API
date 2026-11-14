package dev.ivanvoloshyn.theraflowapi.authentication;

import dev.ivanvoloshyn.theraflowapi.TestContainerConfiguration;
import dev.ivanvoloshyn.theraflowapi.account.AccountRepository;
import dev.ivanvoloshyn.theraflowapi.account.AccountTestBuilder;
import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import dev.ivanvoloshyn.theraflowapi.authentication.model.RefreshToken;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
@Import(TestContainerConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private EntityManager entityManager;

    @DisplayName("""
            findByTokenHash
            SHOULD return persisted RefreshToken WITH Account and expiration
            WHEN matching hash exists
            """)
    @Test
    void findByTokenHash_success() {
        String tokenHash = "a".repeat(64);
        Instant expiresAt = Instant.parse("2030-01-01T12:00:00Z");
        Account account = AccountTestBuilder.therapistVerifiedAccount();
        RefreshToken token = new RefreshToken(tokenHash, expiresAt);
        account.addRefreshToken(token);
        accountRepository.saveAndFlush(account);
        entityManager.clear();

        Optional<RefreshToken> result = refreshTokenRepository.findByTokenHash(tokenHash);

        assertThat(result).hasValueSatisfying(saved -> {
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getTokenHash()).isEqualTo(tokenHash);
            assertThat(saved.getAccount().getId()).isEqualTo(account.getId());
            assertThat(saved.getExpiresAt()).isEqualTo(expiresAt);
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.isRevoked()).isFalse();
        });
    }

    @DisplayName("""
            findByTokenHash
            SHOULD return revoked RefreshToken
            WHEN revocation was persisted
            """)
    @Test
    void findByTokenHash_revokedToken() {
        String tokenHash = "b".repeat(64);
        Account account = AccountTestBuilder.therapistVerifiedAccount();
        RefreshToken token = new RefreshToken(tokenHash, Instant.parse("2030-01-01T12:00:00Z"));
        account.addRefreshToken(token);
        accountRepository.saveAndFlush(account);
        entityManager.clear();

        RefreshToken saved = refreshTokenRepository.findByTokenHash(tokenHash).orElseThrow();
        saved.setRevoked(true);
        entityManager.flush();
        entityManager.clear();

        assertThat(refreshTokenRepository.findByTokenHash(tokenHash))
                .hasValueSatisfying(reloaded -> assertThat(reloaded.isRevoked()).isTrue());
    }

    @DisplayName("""
            findByTokenHash
            SHOULD return empty
            WHEN token hash does not exist
            """)
    @Test
    void findByTokenHash_unknownToken() {
        assertThat(refreshTokenRepository.findByTokenHash("unknown-token-hash")).isEmpty();
    }

    @DisplayName("""
            findAccountIdByTokenHash
            SHOULD resolve the owner before loading or locking the token
            WHEN matching hash exists
            """)
    @Test
    void findAccountIdByTokenHash_success() {
        String tokenHash = "c".repeat(64);
        Account account = AccountTestBuilder.therapistVerifiedAccount();
        account.addRefreshToken(new RefreshToken(tokenHash, Instant.parse("2030-01-01T12:00:00Z")));
        accountRepository.saveAndFlush(account);
        entityManager.clear();

        assertThat(refreshTokenRepository.findAccountIdByTokenHash(tokenHash)).contains(account.getId());
        assertThat(refreshTokenRepository.findAccountIdByTokenHash("unknown-token-hash")).isEmpty();
    }
}
