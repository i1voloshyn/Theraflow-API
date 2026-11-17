package dev.ivanvoloshyn.theraflowapi.therapist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TherapistRepository extends JpaRepository<Therapist, UUID> {


}
