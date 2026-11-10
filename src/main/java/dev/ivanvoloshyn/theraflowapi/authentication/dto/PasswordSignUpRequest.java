package dev.ivanvoloshyn.theraflowapi.authentication.dto;

import  dev.ivanvoloshyn.theraflowapi.account.dto.AccountType;
import dev.ivanvoloshyn.theraflowapi.security.password.Password;
import jakarta.validation.constraints.Email;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record PasswordSignUpRequest(
        @Email
        String email,
        AccountType type,
        @Password
        String password
) {
}
