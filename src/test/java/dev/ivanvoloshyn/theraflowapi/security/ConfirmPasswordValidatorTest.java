package dev.ivanvoloshyn.theraflowapi.security;

import dev.ivanvoloshyn.theraflowapi.authentication.dto.ConfirmPasswordResetRequest;
import dev.ivanvoloshyn.theraflowapi.security.password.ConfirmPassword;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;

class ConfirmPasswordValidatorTest {
    private static final String PASSWORD = "StrongPassword1!";
    private static AnnotationConfigApplicationContext applicationContext;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        applicationContext = new AnnotationConfigApplicationContext();
        TestPropertyValues.of("app.password.length.min=10", "app.password.length.max=64")
                .applyTo(applicationContext);
        applicationContext.register(PasswordValidator.class, LocalValidatorFactoryBean.class);
        applicationContext.refresh();
        validator = applicationContext.getBean(Validator.class);
    }

    @AfterAll
    static void closeContext() {
        applicationContext.close();
    }

    @Test
    void matchingPasswordsAreValid() {
        var request = new ConfirmPasswordResetRequest("token", PASSWORD, PASSWORD);

        assertThat(validator.validate(request)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"DifferentPassword1!", "strongPassword1!", "StrongPassword1! "})
    void mismatchedPasswordsProduceAConfirmationFieldError(String confirmation) {
        var request = new ConfirmPasswordResetRequest("token", PASSWORD, confirmation);

        assertThat(validator.validate(request)).singleElement().satisfies(violation -> {
            assertThat(violation.getPropertyPath().toString()).isEqualTo("confirmPassword");
            assertThat(violation.getMessage()).isEqualTo("Passwords do not match");
            assertThat(violation.getConstraintDescriptor().getAnnotation()).isInstanceOf(ConfirmPassword.class);
        });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void missingConfirmationIsReportedOnlyByNotBlank(String confirmation) {
        var request = new ConfirmPasswordResetRequest("token", PASSWORD, confirmation);

        assertThat(validator.validate(request)).singleElement().satisfies(violation -> {
            assertThat(violation.getPropertyPath().toString()).isEqualTo("confirmPassword");
            assertThat(violation.getConstraintDescriptor().getAnnotation()).isInstanceOf(NotBlank.class);
        });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void missingNewPasswordIsReportedOnlyByNotBlank(String password) {
        var request = new ConfirmPasswordResetRequest("token", password, PASSWORD);

        assertThat(validator.validate(request)).singleElement().satisfies(violation -> {
            assertThat(violation.getPropertyPath().toString()).isEqualTo("newPassword");
            assertThat(violation.getConstraintDescriptor().getAnnotation()).isInstanceOf(NotBlank.class);
        });
    }
}
