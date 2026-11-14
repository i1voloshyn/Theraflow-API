package dev.ivanvoloshyn.theraflowapi.security;

import dev.ivanvoloshyn.theraflowapi.authentication.AuthIdentityRepository;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthIdentity;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthProvider;
import dev.ivanvoloshyn.theraflowapi.security.model.TheraflowUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDetailsServiceImpl implements UserDetailsService {
    private final AuthIdentityRepository authIdentityRepository;

    @Override
    public TheraflowUser loadUserByUsername(String username) {
        String normalizedEmail = username.trim().toLowerCase(Locale.ROOT);
        AuthIdentity identity = authIdentityRepository
                .findByProviderAndProviderUserId(AuthProvider.PASSWORD, normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        return toUser(identity);
    }

    public TheraflowUser loadUserByAccountId(UUID accountId) {
        AuthIdentity identity = authIdentityRepository.findById(accountId)
                .filter(value -> value.getProvider() == AuthProvider.PASSWORD)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        return toUser(identity);
    }

    private TheraflowUser toUser(AuthIdentity identity) {
        var account = identity.getAccount();
        return new TheraflowUser(account.getId(), identity.getEmail(), identity.getPasswordHash(),
                account.getType(), account.getStatus());
    }
}
