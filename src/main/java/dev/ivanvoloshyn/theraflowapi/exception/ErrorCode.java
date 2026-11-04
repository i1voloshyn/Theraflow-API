package dev.ivanvoloshyn.theraflowapi.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    EMAIL_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "Email exists",
            "An account with provided email already exists"

    );

    private final HttpStatus httpStatus;
    private final String title;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String title, String message) {
        this.httpStatus = httpStatus;
        this.title = title;
        this.message = message;
    }
}
