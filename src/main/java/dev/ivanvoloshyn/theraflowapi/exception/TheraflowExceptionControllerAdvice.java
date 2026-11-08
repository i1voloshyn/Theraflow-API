package dev.ivanvoloshyn.theraflowapi.exception;

import dev.ivanvoloshyn.theraflowapi.exception.exceptions.PasswordPolicyException;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.TheraflowApiException;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorResponse;
import dev.ivanvoloshyn.theraflowapi.exception.model.InvalidParam;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

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

        List<InvalidParam> invalidParams = ex.getViolations()
                .stream()
                .map(violation -> new InvalidParam(
                        violation.name().toLowerCase(Locale.ROOT),
                        violation.getMessageTemplate()
                ))
                .toList();
        ErrorResponse error = errorResponse(req, ErrorCode.PASSWORD_WEAK, null, invalidParams);

        return ResponseEntity
                .badRequest()
                .body(error);
    }

    private ErrorResponse errorResponse(HttpServletRequest req,
                                        ErrorCode code,
                                        @Nullable String message,
                                        @Nullable List<InvalidParam> params
    ) {

        URI path = URI.create(req.getRequestURI());
        String errorMessage = message!=null ? message:code.getMessage();

        return new ErrorResponse(code.getHttpStatus().value(), code, code.getTitle(), errorMessage, path, Instant.now(), params);
    }

}
