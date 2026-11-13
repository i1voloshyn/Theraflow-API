package dev.ivanvoloshyn.theraflowapi.authentication;

import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthIdentity;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;
import java.util.UUID;

public interface AuthIdentityRepository extends JpaRepository<AuthIdentity, UUID> {

    boolean existsByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    @EntityGraph(attributePaths = "account")
    Optional<AuthIdentity> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);
}
