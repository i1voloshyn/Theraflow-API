package dev.ivanvoloshyn.theraflowapi.account;

import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    @NativeQuery("""
            SELECT a.id, a.type, a.status, a.created_at, a.updated_at FROM accounts a
            JOIN auth_identities ai on a.id = ai.account_id
            WHERE ai.email = :email AND ai.provider='PASSWORD'
            """)
    Optional<Account> findByEmail(@Param("email") String email);
}
