package dev.ivanvoloshyn.theraflowapi.exception.exceptions;

import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import lombok.Getter;

public class TheraflowApiException extends RuntimeException {
    @Getter
    private final ErrorCode errorCode;


    public TheraflowApiException(ErrorCode code) {
        super(code.getMessage());
        this.errorCode = code;
    }

    public TheraflowApiException(ErrorCode code, Throwable cause) {
        super(code.getMessage(), cause);
        this.errorCode = code;
    }
}
