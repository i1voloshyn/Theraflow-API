package dev.ivanvoloshyn.theraflowapi.practicelocation;

import dev.ivanvoloshyn.theraflowapi.TestContainerConfiguration;
import dev.ivanvoloshyn.theraflowapi.account.AccountRepository;
import dev.ivanvoloshyn.theraflowapi.account.AccountTestBuilder;
import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import dev.ivanvoloshyn.theraflowapi.practicelocation.model.PracticeLocation;
import dev.ivanvoloshyn.theraflowapi.therapist.Therapist;
import dev.ivanvoloshyn.theraflowapi.therapist.TherapistRepository;
import dev.ivanvoloshyn.theraflowapi.therapist.TherapistTestBuilder;
import jakarta.persistence.EntityManager;
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
class PracticeLocationRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TherapistRepository therapistRepository;
    @Autowired
    private PracticeLocationRepository practiceLocationRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistNewPracticeLocation() {
        Account accountToSave = AccountTestBuilder.therapistVerifiedAccount();
        Therapist therapistToSave = TherapistTestBuilder.defaultTestTherapist();
        PracticeLocation locationToSave = defaultTestPracticeLocation();

        Account persistedAccount = accountRepository.save(accountToSave);
        therapistToSave.setAccount(persistedAccount);
        entityManager.flush();
        entityManager.clear();
        Therapist savedTherapist = therapistRepository.findAll().getFirst();
        savedTherapist.addLocation(locationToSave);
        entityManager.flush();
        entityManager.clear();

        assertThat(practiceLocationRepository.findAll().size()).isEqualTo(1);

    }

    private PracticeLocation defaultTestPracticeLocation() {
        return new PracticeLocation.Builder("Therapy Practice",
                "Kraków",
                "Some Street",
                "34A",
                "5")
                .postalCode("30-392")
                .phoneNumber("+48123456789")
                .build();

    }

}