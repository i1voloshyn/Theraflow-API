package dev.ivanvoloshyn.theraflowapi.exception;

public class ResourceConflictException extends TheraflowApiException {
    public ResourceConflictException(ErrorCode code) {
        super(code);
    }
}
