package dev.ivanvoloshyn.theraflowapi.therapist;

import dev.ivanvoloshyn.theraflowapi.TestContainerConfiguration;
import dev.ivanvoloshyn.theraflowapi.account.AccountRepository;
import dev.ivanvoloshyn.theraflowapi.account.AccountTestBuilder;
import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainerConfiguration.class)
class TherapistRepositoryTest {
    @Autowired
    private TherapistRepository therapistRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void tearDown() {
        therapistRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void createTherapistProfile() {
        UUID accountId = saveAccount();
        UUID savedTherapistId = createTherapist(accountId);

        Optional<Therapist> savedTherapist = therapistRepository.findById(savedTherapistId);

        assertTrue(savedTherapist.isPresent());
        assertThat(savedTherapist.get().getId()).isNotNull();
        assertThat(savedTherapist.get().getCreatedAt()).isNotNull();
    }

    @Test
    void findTherapistByAccountId() {
        UUID accountId = saveAccount();
        createTherapist(accountId);

        therapistRepository.findTherapistByAccountId(accountId)
                .ifPresent(therapist -> {
                    assertThat(therapist.getAccount().getId()).isEqualTo(accountId);
                });
    }

    private UUID saveAccount() {
        Account therapistAccount = AccountTestBuilder.therapistUnverifiedAccount();
        Account persistedAccount = accountRepository.save(therapistAccount); // receive an id
        entityManager.flush();
        entityManager.clear();
        return persistedAccount.getId();
    }

    private UUID createTherapist(UUID accountId) {
        Therapist therapistToPersist = defaultTestTherapist();
        accountRepository.findById(accountId)
                .ifPresent(therapistToPersist::setAccount);
        Therapist persisted = therapistRepository.save(therapistToPersist);
        entityManager.flush();
        entityManager.clear();
        return persisted.getId();
    }


    private Therapist defaultTestTherapist() {
        return new Therapist(
                "Joe",
                "Toronto",
                "PL 1234",
                "Just Doc"
        );
    }

}