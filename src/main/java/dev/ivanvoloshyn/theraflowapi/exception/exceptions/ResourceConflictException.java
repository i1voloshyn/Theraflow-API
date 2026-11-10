package dev.ivanvoloshyn.theraflowapi.exception.exceptions;

import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;

public class ResourceConflictException extends TheraflowApiException {
    public ResourceConflictException(ErrorCode code) {
        super(code);
    }
}
