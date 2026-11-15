package dev.ivanvoloshyn.theraflowapi.guardian.model;

import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "guardians")
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class Guardian {
    @Id
    @Setter(AccessLevel.NONE)
    private UUID id;
    private String firstName;
    private String lastName;
    @Generated(event = EventType.INSERT)
    @Column(insertable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(insertable = false)
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "account_id")
    private Account account;
}
