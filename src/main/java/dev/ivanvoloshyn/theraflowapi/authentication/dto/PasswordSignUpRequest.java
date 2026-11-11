package dev.ivanvoloshyn.theraflowapi.authentication.dto;

import dev.ivanvoloshyn.theraflowapi.account.dto.AccountType;
import dev.ivanvoloshyn.theraflowapi.security.password.Password;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record PasswordSignUpRequest(
        @NotBlank
        @Email
        String email,
        AccountType type,
        @Password
        @NotBlank
        String password
) {
}
