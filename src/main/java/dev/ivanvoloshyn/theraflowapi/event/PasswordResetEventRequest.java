package dev.ivanvoloshyn.theraflowapi.event;

public record PasswordResetEventRequest(
        String email,
        String token
) {
}