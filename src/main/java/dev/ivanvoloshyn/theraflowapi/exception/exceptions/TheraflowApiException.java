package dev.ivanvoloshyn.theraflowapi.exception.exceptions;

import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import lombok.Getter;

public class TheraflowApiException extends RuntimeException {
    @Getter
    private final ErrorCode errorCode;

    @Getter
    private Throwable cause = null;

    public TheraflowApiException(ErrorCode code) {
        super(code.getMessage());
        this.errorCode = code;
    }

    public TheraflowApiException(ErrorCode code, Throwable cause) {
        super(code.getMessage());
        this.errorCode = code;
        this.cause = cause;
    }
}
