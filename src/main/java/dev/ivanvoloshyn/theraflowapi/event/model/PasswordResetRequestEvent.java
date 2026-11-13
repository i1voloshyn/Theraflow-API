package dev.ivanvoloshyn.theraflowapi.event.model;

public record PasswordResetRequestEvent(
        String email,
        String token
) {
}