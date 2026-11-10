package dev.ivanvoloshyn.theraflowapi.exception.model;

public record InvalidParam(
        String name,
        String reason
) {
}
