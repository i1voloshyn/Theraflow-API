package dev.ivanvoloshyn.theraflowapi.practicelocation;

import dev.ivanvoloshyn.theraflowapi.practicelocation.model.PracticeLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PracticeLocationRepository extends JpaRepository<PracticeLocation, UUID> {
}
