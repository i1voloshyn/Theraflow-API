package dev.ivanvoloshyn.theraflowapi.authentication.model;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_identities")
@Getter
public class AuthIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(insertable = false, updatable = false)
    private UUID id;
    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;
    @Column(nullable = false)
    private String providerUserId;
    @Column(nullable = false)
    private String email;
    @Nullable
    private String passwordHash;
    @Generated(event = EventType.INSERT)
    @Column(insertable = false)
    private Instant createdAt;
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(insertable = false)
    private Instant updatedAt;

    public AuthIdentity() {
    }

    public AuthIdentity(AuthProvider provider,
                        String providerUserId,
                        String email,
                        @Nullable String passwordHash) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.passwordHash = passwordHash;
    }
}
