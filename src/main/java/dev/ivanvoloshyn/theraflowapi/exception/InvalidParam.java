package dev.ivanvoloshyn.theraflowapi.exception;

public record InvalidParam(
        String name,
        String reason
) {
}
