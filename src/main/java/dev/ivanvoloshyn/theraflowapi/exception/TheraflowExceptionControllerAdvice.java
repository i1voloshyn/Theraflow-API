package dev.ivanvoloshyn.theraflowapi.exception;

import dev.ivanvoloshyn.theraflowapi.exception.exceptions.PasswordPolicyException;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.TheraflowApiException;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorResponse;
import dev.ivanvoloshyn.theraflowapi.exception.model.ValidationError;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@RestControllerAdvice
public class TheraflowExceptionControllerAdvice {

    @ExceptionHandler(TheraflowApiException.class)
    public ResponseEntity<ErrorResponse> handleTheraflowApiException(HttpServletRequest req, TheraflowApiException ex) {
        ErrorCode code = ex.getErrorCode();
        ErrorResponse response = errorResponse(req, code, ex.getMessage(), null);
        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    @ExceptionHandler(PasswordPolicyException.class)
    public ResponseEntity<ErrorResponse> handlePasswordPolicyException(
            PasswordPolicyException ex,
            HttpServletRequest req) {

        List<ValidationError> errors = ex.getViolations()
                .stream()
                .map(violation -> new ValidationError(
                        "password",
                        violation.name().toLowerCase(Locale.ROOT),
                        violation.getMessageTemplate()
                ))
                .toList();
        ErrorResponse error = errorResponse(req, ErrorCode.PASSWORD_WEAK, null, errors);

        return ResponseEntity
                .status(error.status())
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest req
    ) {
        var errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> {
                    String fieldName = error.getField();
                    ErrorCode code = resolveErrorCode(Objects.requireNonNull(error.getCode()));
                    return new ValidationError(fieldName, code.name(), code.getMessage());
                }).toList();
        ErrorResponse response = errorResponse(req, ErrorCode.VALIDATION_FAILED, null, errors);
        return ResponseEntity.badRequest().body(response);
    }

    private ErrorResponse errorResponse(HttpServletRequest req,
                                        ErrorCode code,
                                        @Nullable String message,
                                        @Nullable List<ValidationError> params
    ) {

        URI path = URI.create(req.getRequestURI());
        String errorMessage = message!=null ? message:code.getMessage();

        return new ErrorResponse(code, code.getHttpStatus().value(), errorMessage, path, Instant.now(), params);
    }

    private ErrorCode resolveErrorCode(String fieldError) {
        return switch (fieldError) {
            case "Email" -> ErrorCode.EMAIL_INVALID;
            case "NotBlank" -> ErrorCode.REQUIRED_FIELD;
            default -> ErrorCode.THERAFLOW_ERROR;
        };
    }

}
