package dev.ivanvoloshyn.theraflowapi.authentication.dto;

import dev.ivanvoloshyn.theraflowapi.account.dto.AccountStatus;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountType;

import java.util.UUID;

public record SignUpResponse(
        UUID id,
        AccountType type,
        AccountStatus status
) {
}
