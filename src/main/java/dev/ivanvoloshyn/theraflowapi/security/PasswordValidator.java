package dev.ivanvoloshyn.theraflowapi.security;

import dev.ivanvoloshyn.theraflowapi.exception.exceptions.PasswordPolicyException;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import dev.ivanvoloshyn.theraflowapi.security.password.Password;
import dev.ivanvoloshyn.theraflowapi.security.password.PasswordViolation;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
@Component
public class PasswordValidator implements ConstraintValidator<Password, String> {
    @Value("${password.length.min}")
    private int passwordMinLength;

    @Value("${password.length.max}")
    private int passwordMaxLength;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return validate(value);
    }

    private boolean validate(String password) {
        Set<PasswordViolation> violations = new HashSet<>();
        checkPasswordLength(password, violations);

        if (!containsSpecialCharacter(password)) {
            violations.add(PasswordViolation.MISSING_SPECIAL_CHARACTER);
        }

        if (!containsUppercaseLetter(password)) {
            violations.add(PasswordViolation.MISSING_UPPERCASE_LETTER);
        }

        if (!containsDigit(password)) {
            violations.add(PasswordViolation.MISSING_NUMBER);
        }

        if (!violations.isEmpty()) {
            throw new PasswordPolicyException(ErrorCode.PASSWORD_WEAK, violations);
        }

        return true;
    }

    private void checkPasswordLength(String password, Set<PasswordViolation> violations) {
        if (password.length() < passwordMinLength) {
            violations.add(PasswordViolation.TOO_SHORT);
        }
        if (password.length() > passwordMaxLength) {
            violations.add(PasswordViolation.TOO_LONG);
        }
    }

    private boolean containsSpecialCharacter(String password) {
        return password.codePoints()
                .anyMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c));
    }

    private boolean containsUppercaseLetter(String password) {
        return password.codePoints()
                .anyMatch(Character::isUpperCase);
    }

    private boolean containsDigit(String password) {
        return password.codePoints()
                .anyMatch(Character::isDigit);
    }

}
