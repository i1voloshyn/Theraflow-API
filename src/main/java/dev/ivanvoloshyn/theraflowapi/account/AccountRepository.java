package dev.ivanvoloshyn.theraflowapi.account;

import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthProvider;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            SELECT a FROM Account a
            JOIN a.authIdentity ai
            WHERE ai.email = :email AND ai.provider= :provider
            """)
    Optional<Account> findByEmailAndProvider(@Param("email") String email, @Param("provider") AuthProvider provider);
}
