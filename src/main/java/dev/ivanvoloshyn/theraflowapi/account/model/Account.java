package dev.ivanvoloshyn.theraflowapi.account.model;

import dev.ivanvoloshyn.theraflowapi.account.dto.AccountStatus;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountType;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthIdentity;
import dev.ivanvoloshyn.theraflowapi.authentication.model.PasswordResetToken;
import dev.ivanvoloshyn.theraflowapi.authentication.model.RefreshToken;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private AccountType type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Setter
    private AccountStatus status;
    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private Instant createdAt;
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(insertable = false, updatable = false)
    private Instant updatedAt;

    @OneToOne(mappedBy = "account", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private AuthIdentity authIdentity;

    @OneToMany(mappedBy = "account", cascade = CascadeType.PERSIST)
    private Set<PasswordResetToken> passwordResetTokens = new HashSet<>();

    @OneToMany(mappedBy = "account", cascade = CascadeType.PERSIST)
    private Set<RefreshToken> refreshTokens = new HashSet<>();

    public Account() {
    }

    public Account(AccountType type, AccountStatus status) {
        this.type = type;
        this.status = status;
    }

    public void setAuthIdentity(AuthIdentity identity) {
        authIdentity = identity;
        identity.setAccount(this);
    }

    public void addPasswordResetToken(PasswordResetToken token) {
        passwordResetTokens.add(token);
        token.setAccount(this);
    }

    public void addRefreshToken(RefreshToken token) {
        refreshTokens.add(token);
        token.setAccount(this);
    }

}
