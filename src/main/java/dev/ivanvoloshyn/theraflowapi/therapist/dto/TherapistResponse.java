package dev.ivanvoloshyn.theraflowapi.therapist.dto;

import java.util.UUID;

public record TherapistResponse(
        UUID id,
        String firstName,
        String lastName,
        String licenseNumber,
        String professionalTitle
) {
}
