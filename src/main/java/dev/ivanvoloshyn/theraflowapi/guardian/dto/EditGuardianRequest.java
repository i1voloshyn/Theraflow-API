package dev.ivanvoloshyn.theraflowapi.guardian.dto;

import jakarta.validation.constraints.NotBlank;

public record EditGuardianRequest(
        @NotBlank
        String firstName,
        @NotBlank
        String lastName
) {
}
