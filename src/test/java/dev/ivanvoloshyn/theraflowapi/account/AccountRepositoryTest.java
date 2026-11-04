package dev.ivanvoloshyn.theraflowapi.account;

import dev.ivanvoloshyn.theraflowapi.TestContainerConfiguration;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountStatus;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountType;
import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainerConfiguration.class)
class AccountRepositoryTest {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("save should return a persisted account with a generated ID and timestamps when the account is new")
    void saveNewAccount() {
        Account account = new Account(AccountType.THERAPIST, AccountStatus.PENDING_VERIFICATION);

        Account savedAccount = accountRepository.save(account);
        entityManager.flush();
        entityManager.clear();

        assertThat(savedAccount.getCreatedAt()).isNotNull();
        assertThat(savedAccount.getUpdatedAt()).isNotNull();

        Account persistedAccount = entityManager.find(Account.class, savedAccount.getId());
        assertThat(persistedAccount).isNotNull();
        assertThat(persistedAccount.getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
        assertThat(persistedAccount.getCreatedAt()).isEqualTo(savedAccount.getCreatedAt());
        assertThat(persistedAccount.getUpdatedAt()).isEqualTo(savedAccount.getUpdatedAt());
    }

}
