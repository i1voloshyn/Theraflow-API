package dev.ivanvoloshyn.theraflowapi.therapist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;
import java.util.UUID;

public interface TherapistRepository extends JpaRepository<Therapist, UUID> {

    @Query("""
            SELECT t FROM Therapist t WHERE t.account.id = :accountId
            """)
    Optional<Therapist> findTherapistByAccountId(@Param("accountId") UUID accountId);
}
