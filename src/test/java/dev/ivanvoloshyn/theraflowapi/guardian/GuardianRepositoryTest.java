package dev.ivanvoloshyn.theraflowapi.guardian;

import dev.ivanvoloshyn.theraflowapi.TestContainerConfiguration;
import dev.ivanvoloshyn.theraflowapi.account.AccountRepository;
import dev.ivanvoloshyn.theraflowapi.account.AccountTestBuilder;
import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import dev.ivanvoloshyn.theraflowapi.authentication.AuthIdentityTestBuilder;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthIdentity;
import dev.ivanvoloshyn.theraflowapi.guardian.model.Guardian;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainerConfiguration.class)
class GuardianRepositoryTest {
    @Autowired
    private GuardianRepository guardianRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void mappingTest() {
        Account guardianAccount = AccountTestBuilder.guardianVerifiedAccount();
        AuthIdentity identity = AuthIdentityTestBuilder.passwordProviderAuthIdentity();

        guardianAccount.setAuthIdentity(identity);
        accountRepository.save(guardianAccount);

        Guardian guardian = new Guardian();
        guardian.setFirstName("Joe");
        guardian.setLastName("Canada");
        guardian.setAccount(guardianAccount);

        guardianRepository.save(guardian);
        entityManager.flush();
        entityManager.clear();

        Optional<Guardian> saved = guardianRepository.findById(guardianAccount.getId());

        assertThat(saved).isPresent();
    }

}