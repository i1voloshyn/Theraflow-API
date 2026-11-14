package dev.ivanvoloshyn.theraflowapi.security;

import dev.ivanvoloshyn.theraflowapi.account.AccountTestBuilder;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountStatus;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountType;
import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import dev.ivanvoloshyn.theraflowapi.authentication.AuthIdentityRepository;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthIdentity;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthProvider;
import dev.ivanvoloshyn.theraflowapi.security.model.TheraflowUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.ACCOUNT_ID;
import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.DEFAULT_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private AuthIdentityRepository authIdentityRepository;
    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @DisplayName("""
            loadUserByUsername
            SHOULD normalize email and return account details with authorities
            WHEN a password identity exists
            """)
    @Test
    void loadUserByUsername_success() {
        Account account = AccountTestBuilder.therapistVerifiedAccount();
        ReflectionTestUtils.setField(account, "id", ACCOUNT_ID);
        AuthIdentity identity = new AuthIdentity(AuthProvider.PASSWORD, DEFAULT_EMAIL, DEFAULT_EMAIL, "password-hash");
        account.setAuthIdentity(identity);

        when(authIdentityRepository.findByProviderAndProviderUserId(AuthProvider.PASSWORD, DEFAULT_EMAIL))
                .thenReturn(Optional.of(identity));

        TheraflowUser user = userDetailsService.loadUserByUsername("  TEST@GMAIL.COM  ");

        assertThat(user.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(user.getUsername()).isEqualTo(DEFAULT_EMAIL);
        assertThat(user.getPassword()).isEqualTo("password-hash");
        assertThat(user.getType()).isEqualTo(AccountType.THERAPIST);
        assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(user.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_THERAPIST");
        verify(authIdentityRepository).findByProviderAndProviderUserId(AuthProvider.PASSWORD, DEFAULT_EMAIL);
    }

    @DisplayName("""
            loadUserByUsername
            SHOULD throw UsernameNotFoundException
            WHEN no password identity exists for the normalized email
            """)
    @Test
    void loadUserByUsername_error() {
        when(authIdentityRepository.findByProviderAndProviderUserId(AuthProvider.PASSWORD, DEFAULT_EMAIL))
                .thenReturn(Optional.empty());

        assertThatExceptionOfType(UsernameNotFoundException.class)
                .isThrownBy(() -> userDetailsService.loadUserByUsername("  TEST@GMAIL.COM  "));

        verify(authIdentityRepository).findByProviderAndProviderUserId(AuthProvider.PASSWORD, DEFAULT_EMAIL);
    }

    @DisplayName("""
            loadUserByAccountId
            SHOULD return the current account status
            WHEN the account is disabled after a token was issued
            """)
    @Test
    void loadUserByAccountId_success() {
        Account account = new Account(AccountType.THERAPIST, AccountStatus.DISABLED);
        ReflectionTestUtils.setField(account, "id", ACCOUNT_ID);
        AuthIdentity identity = new AuthIdentity(AuthProvider.PASSWORD, DEFAULT_EMAIL, DEFAULT_EMAIL, "password-hash");
        account.setAuthIdentity(identity);

        when(authIdentityRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(identity));

        TheraflowUser user = userDetailsService.loadUserByAccountId(ACCOUNT_ID);

        assertThat(user.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(user.getStatus()).isEqualTo(AccountStatus.DISABLED);
        assertThat(user.getUsername()).isEqualTo(DEFAULT_EMAIL);
        verify(authIdentityRepository).findById(ACCOUNT_ID);
    }

    @DisplayName("""
            loadUserByAccountId
            SHOULD throw UsernameNotFoundException
            WHEN the account identity does not exist
            """)
    @Test
    void loadUserByAccountId_error() {
        when(authIdentityRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatExceptionOfType(UsernameNotFoundException.class)
                .isThrownBy(() -> userDetailsService.loadUserByAccountId(ACCOUNT_ID));
    }

    @DisplayName("""
            eraseCredentials
            SHOULD remove the password hash
            AND preserve the authenticated account details
            """)
    @Test
    void eraseCredentials_success() {
        TheraflowUser user = new TheraflowUser(ACCOUNT_ID, DEFAULT_EMAIL, "password-hash",
                AccountType.THERAPIST, AccountStatus.ACTIVE);

        user.eraseCredentials();

        assertThat(user.getPassword()).isNull();
        assertThat(user.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(user.getUsername()).isEqualTo(DEFAULT_EMAIL);
        assertThat(user.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_THERAPIST");
    }
}
