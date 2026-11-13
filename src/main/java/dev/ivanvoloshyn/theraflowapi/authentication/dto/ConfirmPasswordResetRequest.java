package dev.ivanvoloshyn.theraflowapi.authentication.dto;

import dev.ivanvoloshyn.theraflowapi.security.password.ConfirmPassword;
import dev.ivanvoloshyn.theraflowapi.security.password.Password;
import jakarta.validation.constraints.NotBlank;

@ConfirmPassword
public record ConfirmPasswordResetRequest(
        @NotBlank String token,
        @NotBlank
        @Password
        String newPassword,
        @NotBlank
        String confirmPassword
) {
}
