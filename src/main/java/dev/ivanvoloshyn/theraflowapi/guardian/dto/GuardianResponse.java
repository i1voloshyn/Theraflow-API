package dev.ivanvoloshyn.theraflowapi.guardian.dto;

import java.util.UUID;

public record GuardianResponse(
        UUID id,
        String firstName,
        String lastName
) {
}
