package dev.ivanvoloshyn.theraflowapi.authentication.dto;

import dev.ivanvoloshyn.theraflowapi.account.dto.AccountType;
import jakarta.validation.constraints.Email;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record SignUpRequest(
        @Email
        String email,
        AccountType type
) {
}
