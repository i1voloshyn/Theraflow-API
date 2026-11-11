package dev.ivanvoloshyn.theraflowapi.exception.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;

import java.net.URI;
import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        ErrorCode code,
        int status,
        String message,
        URI path,
        Instant timestamp,
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<ValidationError> errors
) {
}
