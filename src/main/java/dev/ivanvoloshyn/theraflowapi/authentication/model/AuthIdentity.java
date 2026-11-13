package dev.ivanvoloshyn.theraflowapi.authentication.model;

import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_identities")
@Getter
public class AuthIdentity {
    @Id
    @Column(name = "account_id")
    private UUID id;
    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private AuthProvider provider;
    @Column(nullable = false)
    private String providerUserId;
    @Column(nullable = false)
    private String email;
    @Nullable
    @Setter
    private String passwordHash;
    @Generated(event = EventType.INSERT)
    @Column(insertable = false)
    private Instant createdAt;
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(insertable = false)
    private Instant updatedAt;

    @OneToOne
    @JoinColumn(name = "account_id", referencedColumnName = "id")
    @MapsId
    @Setter
    private Account account;

    public AuthIdentity() {
    }

    public AuthIdentity(
            AuthProvider provider,
            String providerUserId,
            String email,
            @Nullable String passwordHash) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.passwordHash = passwordHash;
    }
}
