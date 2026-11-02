package dev.ivanvoloshyn.theraflowapi.account.model;

import dev.ivanvoloshyn.theraflowapi.account.dto.AccountStatus;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(insertable = false, updatable = false)
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter
    private AccountStatus status;
    @Generated(event = EventType.INSERT)
    @Column(insertable = false)
    private Instant createdAt;
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(insertable = false)
    private Instant updatedAt;

    public Account() {
    }

    public Account(AccountType type, AccountStatus status) {
        this.type = type;
        this.status = status;
    }

}
