package dev.ivanvoloshyn.theraflowapi.exception.model;

public record ValidationError(
        String field,
        String code,
        String message
) {
}
