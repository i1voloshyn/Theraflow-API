package dev.ivanvoloshyn.theraflowapi.authentication;

import dev.ivanvoloshyn.theraflowapi.authentication.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    @Query("SELECT t.account.id FROM PasswordResetToken t WHERE t.tokenHash = :tokenHash")
    Optional<UUID> findAccountIdByTokenHash(@Param("tokenHash") String tokenHash);

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
