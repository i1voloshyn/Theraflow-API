package dev.ivanvoloshyn.theraflowapi.exception.exceptions;

import dev.ivanvoloshyn.theraflowapi.security.password.PasswordViolation;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import lombok.Getter;

import java.util.Set;

public class PasswordPolicyException extends TheraflowApiException {
    @Getter
    private final Set<PasswordViolation> violations;

    public PasswordPolicyException(ErrorCode code, Set<PasswordViolation> violations) {
        super(code);
        this.violations = violations;
    }
}
