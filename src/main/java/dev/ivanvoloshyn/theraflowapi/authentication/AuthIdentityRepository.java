package dev.ivanvoloshyn.theraflowapi.authentication;

import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthIdentity;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuthIdentityRepository extends JpaRepository<AuthIdentity, UUID> {

    boolean existsByProviderAndProviderUserId(AuthProvider provider, String providerUserId);
}
