package dev.ivanvoloshyn.theraflowapi.exception.model;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.io.Serializable;

@Getter
public enum ErrorCode {
    AUTHENTICATION_FAILED(
            HttpStatus.UNAUTHORIZED,
            "Invalid email or password"),

    AUTHENTICATION_REQUIRED(
            HttpStatus.UNAUTHORIZED,
            "Authentication is required"),

    ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "You do not have permission to access this resource"),

    EMAIL_NOT_VERIFIED(
            HttpStatus.UNAUTHORIZED,
            "Please verify your email before signing in"),

    REFRESH_TOKEN_INVALID(
            HttpStatus.UNAUTHORIZED,
            "The refresh token is invalid, expired, or has already been used"),

    ACCOUNT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "No account exists for the given identifier"),

    ACCOUNT_DISABLED(
            HttpStatus.FORBIDDEN,
            "This account has been disabled. Please contact support."),

    EMAIL_INVALID(
            HttpStatus.BAD_REQUEST,
            "Please provide a valid email address"
    ),
    REQUIRED_FIELD(
            HttpStatus.BAD_REQUEST,
            "Current field is required"
    ),

    EMAIL_ALREADY_VERIFIED(
            HttpStatus.CONFLICT,
            "The email address has already been verified. Please log in."
    ),

    EMAIL_VERIFICATION_LINK_INVALID(
            HttpStatus.UNAUTHORIZED,
            "The email verification link is broken or has expired. Please request a new verification email"),

    THERAFLOW_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "See logs for details"),

    TOKEN_EXPIRED(
            HttpStatus.UNAUTHORIZED,
            "The provided tokens has expired. Please log in again or use a refresh tokens to obtain a new one"),

    TOKEN_INVALID(
            HttpStatus.UNAUTHORIZED,
            "The provided tokens is invalid or malformed. Please log in again"),

    PASSWORD_WEAK(
            HttpStatus.BAD_REQUEST,
            "Password does not meet security requirements. Use at least 10 characters with uppercase, lowercase, numbers, and symbols"),

    PASSWORD_NOT_MATCH(
            HttpStatus.BAD_REQUEST,
            "Passwords do not match"),

    EMAIL_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "An account with provided email already exists"),

    VALIDATION_FAILED(
            HttpStatus.BAD_REQUEST,
            "One or more fields are invalid. See 'errors' for details"),
    PASSWORD_RESET_TOKEN_INVALID(
            HttpStatus.BAD_REQUEST,
            "The password reset link is invalid or has expired. Please request a new password reset link.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
