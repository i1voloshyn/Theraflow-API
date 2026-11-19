package dev.ivanvoloshyn.theraflowapi.practicelocation.model;

import dev.ivanvoloshyn.theraflowapi.therapist.Therapist;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.Instant;
import java.util.UUID;

@Table(name = "practice_locations")
@Entity
@Getter
@Setter
@NoArgsConstructor
public class PracticeLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;
    private String name;
    private String city;
    private String postalCode;
    private String street;
    private String buildingNumber;
    private String unitNumber;
    private String phoneNumber;
    @Column(insertable = false)
    @Generated(event = EventType.INSERT)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;
    @Column(insertable = false)
    @Setter(AccessLevel.NONE)
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "therapist_id", referencedColumnName = "id")
    private Therapist therapist;

    public static class Builder {
        private final String name;
        private final String city;
        private final String street;
        private final String buildingNumber;
        private final String unitNumber;

        private String postalCode = "";
        private String phoneNumber = "";

        public Builder(String name, String city, String street, String buildingNumber, String unitNumber) {
            this.name = name;
            this.city = city;
            this.street = street;
            this.buildingNumber = buildingNumber;
            this.unitNumber = unitNumber;
        }

        public Builder postalCode(String code) {
            this.postalCode = code;
            return this;
        }

        public Builder phoneNumber(String number) {
            this.phoneNumber = number;
            return this;
        }

        public PracticeLocation build() {
            return new PracticeLocation(this);
        }
    }

    private PracticeLocation(Builder builder) {
        this.name = builder.name;
        this.city = builder.city;
        this.postalCode = builder.postalCode;
        this.street = builder.street;
        this.buildingNumber = builder.buildingNumber;
        this.unitNumber = builder.unitNumber;
        this.phoneNumber = builder.phoneNumber;
    }
}
