package dev.ivanvoloshyn.theraflowapi.security;

import dev.ivanvoloshyn.theraflowapi.authentication.dto.ConfirmPasswordResetRequest;
import dev.ivanvoloshyn.theraflowapi.security.password.ConfirmPassword;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ConfirmPasswordValidator implements ConstraintValidator<ConfirmPassword, ConfirmPasswordResetRequest> {
    @Override
    public boolean isValid(ConfirmPasswordResetRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        String password = request.newPassword();
        String confirmation = request.confirmPassword();

        if (password == null || password.isBlank() || confirmation == null || confirmation.isBlank()) {
            return true;
        }

        if (password.equals(confirmation)) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("confirmPassword")
                .addConstraintViolation();

        return false;
    }
}
