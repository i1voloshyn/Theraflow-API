package dev.ivanvoloshyn.theraflowapi.event.model;

import java.util.UUID;

public record EmailVerificationRequest(
        UUID accountId,
        String email,
        String token
) {
}
