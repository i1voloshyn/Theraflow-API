package dev.ivanvoloshyn.theraflowapi.therapist.dto;

import jakarta.validation.constraints.NotBlank;

public record TherapistRequest(
        @NotBlank
        String firstName,
        @NotBlank
        String lastName,
        //probably, it is possible to create some sort of validation for license number
        @NotBlank
        String licenseNumber,
        @NotBlank
        String professionalTitle
) {
}
