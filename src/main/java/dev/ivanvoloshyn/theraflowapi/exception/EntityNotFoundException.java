package dev.ivanvoloshyn.theraflowapi.exception;

import dev.ivanvoloshyn.theraflowapi.exception.exceptions.TheraflowApiException;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;

public class EntityNotFoundException extends TheraflowApiException {
    public EntityNotFoundException(ErrorCode code) {
        super(code);
    }
}
