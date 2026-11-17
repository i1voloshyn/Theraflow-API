package dev.ivanvoloshyn.theraflowapi.therapist;

import dev.ivanvoloshyn.theraflowapi.TestContainerConfiguration;
import dev.ivanvoloshyn.theraflowapi.account.AccountRepository;
import dev.ivanvoloshyn.theraflowapi.account.AccountTestBuilder;
import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void createProfile() {
        Account therapistAccount = AccountTestBuilder.therapistUnverifiedAccount();
        accountRepository.save(therapistAccount);
        entityManager.flush();
        entityManager.clear();

        Account savedAccount = accountRepository.findAll().getFirst();
        Therapist therapist = defaultTestTherapist();
        therapist.setAccount(savedAccount);
        therapistRepository.save(therapist);
        entityManager.flush();
        entityManager.clear();

        Optional<Therapist> savedTherapist = therapistRepository.findById(savedAccount.getId());

        assertTrue(savedTherapist.isPresent());
        assertThat(savedTherapist.get().getCreatedAt()).isNotNull();

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