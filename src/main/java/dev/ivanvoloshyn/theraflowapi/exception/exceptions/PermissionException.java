package dev.ivanvoloshyn.theraflowapi.exception.exceptions;

import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;

public class PermissionException extends TheraflowApiException{

    public PermissionException(ErrorCode code, Throwable cause) {
        super(code, cause);
    }
}
