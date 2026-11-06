package dev.ivanvoloshyn.theraflowapi.authentication;

import dev.ivanvoloshyn.theraflowapi.TestContainerConfiguration;
import dev.ivanvoloshyn.theraflowapi.account.AccountRepository;
import dev.ivanvoloshyn.theraflowapi.account.AccountTestBuilder;
import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthIdentity;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@DataJpaTest
@Import(TestContainerConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthIdentityRepositoryTest {

    @Autowired
    private AuthIdentityRepository authIdentityRepository;
    @Autowired
    private AccountRepository accountRepository;

    @DisplayName("""
            existsByProviderAndProviderUserId
            SHOULD return true
            WHEN identity exists
            FOR provided data
            """)
    @Test
    void existsByProviderAndProviderUserId_1() {
        Account accountToSave = AccountTestBuilder.therapistUnverifiedAccount();
        AuthIdentity authIdentityToSave = AuthIdentityTestBuilder.passwordProviderAuthIdentity();

        Account savedAccount = accountRepository.save(accountToSave);
        savedAccount.setAuthIdentity(authIdentityToSave);

        authIdentityRepository.save(authIdentityToSave);

        boolean exists = authIdentityRepository.existsByProviderAndProviderUserId(
                authIdentityToSave.getProvider(),
                authIdentityToSave.getProviderUserId());

        assertTrue(exists);
    }

    @DisplayName("""
            existsByProviderAndProviderUserId
            SHOULD return false
            WHEN identity do not exists
            FOR provided data
            """)
    @Test
    void existsByProviderAndProviderUserId_2() {
        AuthProvider provider = AuthProvider.PASSWORD;
        String newEmail = "new_email@gmail.com";

        Account accountToSave = AccountTestBuilder.therapistUnverifiedAccount();
        AuthIdentity authIdentityToSave = AuthIdentityTestBuilder.passwordProviderAuthIdentity();

        Account savedAccount = accountRepository.save(accountToSave);
        savedAccount.setAuthIdentity(authIdentityToSave);

        authIdentityRepository.save(authIdentityToSave);

        boolean exists = authIdentityRepository.existsByProviderAndProviderUserId(provider, newEmail);

        assertFalse(exists);
    }

}