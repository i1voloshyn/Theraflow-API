package dev.ivanvoloshyn.theraflowapi.security.model;

import dev.ivanvoloshyn.theraflowapi.account.dto.AccountStatus;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class TheraflowUser implements UserDetails, CredentialsContainer {
    private final UUID accountId;
    private final String username;
    private String password;
    private final AccountType type;
    private final AccountStatus status;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Set.of(new SimpleGrantedAuthority("ROLE_" + type.name()));
    }

    @Override
    public void eraseCredentials() {
        password = null;
    }
}
