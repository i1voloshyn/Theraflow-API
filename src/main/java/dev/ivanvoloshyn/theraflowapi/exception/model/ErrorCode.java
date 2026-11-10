package dev.ivanvoloshyn.theraflowapi.exception.model;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    THERAFLOW_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Theraflow app error",
            "See logs for details"),

    TOKEN_EXPIRED(
            HttpStatus.UNAUTHORIZED,
            "Token expired",
            "The provided tokens has expired. Please log in again or use a refresh tokens to obtain a new one"),

    TOKEN_INVALID(
            HttpStatus.UNAUTHORIZED,
            "Token invalid",
            "The provided tokens is invalid or malformed. Please log in again"),

    PASSWORD_WEAK(
            HttpStatus.BAD_REQUEST,
            "Password weak",
            "Password does not meet security requirements. Use at least 10 characters with uppercase, lowercase, numbers, and symbols"),

    EMAIL_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "Email exists",
            "An account with provided email already exists"),

    VALIDATION_FAILED
            (HttpStatus.BAD_REQUEST,
                    "Validation failed",
                    "One or more fields are invalid. See 'errors' for details");

    private final HttpStatus httpStatus;
    private final String title;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String title, String message) {
        this.httpStatus = httpStatus;
        this.title = title;
        this.message = message;
    }
}
