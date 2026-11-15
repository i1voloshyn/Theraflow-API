package dev.ivanvoloshyn.theraflowapi.guardian;

import dev.ivanvoloshyn.theraflowapi.guardian.model.Guardian;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GuardianRepository extends JpaRepository<Guardian, UUID> {
}
