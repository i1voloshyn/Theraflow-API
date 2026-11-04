package dev.ivanvoloshyn.theraflowapi.exception;

import lombok.Getter;

public class TheraflowApiException extends RuntimeException {
    @Getter
    private final ErrorCode errorCode;

    public TheraflowApiException(ErrorCode code) {
        super(code.getMessage());
        this.errorCode = code;
    }
}
