package dev.ivanvoloshyn.theraflowapi.authentication.dto;

import  dev.ivanvoloshyn.theraflowapi.account.dto.AccountType;
import jakarta.validation.constraints.Email;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record PasswordSignUpRequest(
        @Email
        String email,
        AccountType type,
        String password
) {
}
