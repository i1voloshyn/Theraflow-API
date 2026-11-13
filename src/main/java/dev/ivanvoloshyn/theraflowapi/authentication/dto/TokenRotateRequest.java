package dev.ivanvoloshyn.theraflowapi.authentication.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRotateRequest(@NotBlank String refreshToken) {
}
