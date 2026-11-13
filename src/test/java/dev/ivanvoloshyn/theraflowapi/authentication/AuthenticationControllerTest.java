package dev.ivanvoloshyn.theraflowapi.authentication;

import dev.ivanvoloshyn.theraflowapi.authentication.dto.ConfirmPasswordResetRequest;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.PasswordResetRequest;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.PasswordSignUpRequest;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.SignUpResponse;
import dev.ivanvoloshyn.theraflowapi.config.SecurityConfiguration;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.PermissionException;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.TheraflowApiException;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorResponse;
import dev.ivanvoloshyn.theraflowapi.fixtures.SignUpTestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.CONFIRM_PASSWORD_RESET_ENDPOINT;
import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.CORRECT_PASSWORD;
import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.DEFAULT_EMAIL;
import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.PASSWORD_RESET_ENDPOINT;
import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.SIGN_UP_ENDPOINT;
import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.TEST_UUID;
import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.VERIFY_EMAIL_ENDPOINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AuthenticationController.class)
@Import({SecurityConfiguration.class})
class AuthenticationControllerTest {

    @MockitoBean
    private PasswordAuthenticationService passwordAuthenticationService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    @DisplayName("""
            signUp
            SHOULD successfully create new account
            RETURN SignUpResponse
            FOR valid request
            """)
    @Test
    void signUp_success() throws Exception {
        PasswordSignUpRequest request = SignUpTestBuilder.defaultTherapistSignUpRequest();

        SignUpResponse expectedResponse = SignUpTestBuilder.defaultTherapistSignUpResponse();

        when(passwordAuthenticationService.signUp(request)).thenReturn(expectedResponse);

        MvcResult mvcResult = mockMvc.perform(post(SIGN_UP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        SignUpResponse actualResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), SignUpResponse.class);

        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(passwordAuthenticationService).signUp(request);
    }

    @DisplayName("""
            signUp
            SHOULD return BAD_REQUEST
            WITH error_code WEAK_PASSWORD
            FOR weak password
            """)
    @Test
    void signUp_error() throws Exception {
        PasswordSignUpRequest request = SignUpTestBuilder.weakPasswordSignUpRequest();

        MvcResult result = mockMvc.perform(post(SIGN_UP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse error = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);
        assertThat(error.code().equals(ErrorCode.PASSWORD_WEAK));

        verifyNoInteractions(passwordAuthenticationService);
    }

    @DisplayName("""
            signUp
            SHOULD return BAD_REQUEST
            WITH error_code VALIDATION_FAILED
            FOR invalid email
            """)
    @Test
    void signUp_error1() throws Exception {
        PasswordSignUpRequest request = SignUpTestBuilder.invalidEmailSignUpRequest();

        MvcResult result = mockMvc.perform(post(SIGN_UP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse errorResponse = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        assertThat(errorResponse.code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
        assertThat(errorResponse.errors().getFirst().code()).isEqualTo(ErrorCode.EMAIL_INVALID.name());

        verifyNoInteractions(passwordAuthenticationService);
    }

    @DisplayName("""
            verifyEmail
            SHOULD return NO_CONTENT
            FOR successful email verification
            """)
    @Test
    void emailVerification_success() throws Exception {
        String token = "valid-verification-access";
        mockMvc.perform(get(VERIFY_EMAIL_ENDPOINT)
                        .param("token", token)
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(passwordAuthenticationService).verifyEmail(token);
    }

    @DisplayName("""
            requestPasswordReset
            SHOULD return OK
            WITH a generic password reset message
            FOR valid request
            """)
    @Test
    void requestPasswordReset_success() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest(DEFAULT_EMAIL);

        mockMvc.perform(post(PASSWORD_RESET_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("If the email is registered, you'll get a reset link"));

        verify(passwordAuthenticationService).requestPasswordReset(request.email());
    }

    @DisplayName("""
            requestPasswordReset
            SHOULD return BAD_REQUEST
            WITH error_code VALIDATION_FAILED
            FOR invalid email
            """)
    @Test
    void requestPasswordReset_error() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest("invalid-email");

        MvcResult result = mockMvc.perform(post(PASSWORD_RESET_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        ErrorResponse error = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        assertThat(error.code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
        assertThat(error.errors()).singleElement().satisfies(validationError -> {
            assertThat(validationError.field()).isEqualTo("email");
            assertThat(validationError.code()).isEqualTo(ErrorCode.EMAIL_INVALID.name());
        });
        verifyNoInteractions(passwordAuthenticationService);
    }

    @DisplayName("""
            requestPasswordReset
            SHOULD return BAD_REQUEST
            WITH error_code VALIDATION_FAILED
            FOR empty email
            """)
    @Test
    void requestPasswordReset_error1() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest("");

        MvcResult result = mockMvc.perform(post(PASSWORD_RESET_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        ErrorResponse error = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        assertThat(error.code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
        assertThat(error.errors()).singleElement().satisfies(validationError -> {
            assertThat(validationError.field()).isEqualTo("email");
            assertThat(validationError.code()).isEqualTo(ErrorCode.REQUIRED_FIELD.name());
        });
        verifyNoInteractions(passwordAuthenticationService);
    }

    @DisplayName("""
            validatePasswordResetLink
            SHOULD return OK
            WITH an empty response body
            FOR valid token
            """)
    @Test
    void validatePasswordResetLink_success() throws Exception {
        String token = TEST_UUID;

        mockMvc.perform(get(PASSWORD_RESET_ENDPOINT)
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(passwordAuthenticationService).validatePasswordResetLink(token);
    }

    @DisplayName("""
            validatePasswordResetLink
            SHOULD return BAD_REQUEST
            WITH error_code PASSWORD_RESET_TOKEN_INVALID
            FOR invalid token
            """)
    @Test
    void validatePasswordResetLink_error() throws Exception {
        String token = TEST_UUID;

        doThrow(new PermissionException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID))
                .when(passwordAuthenticationService).validatePasswordResetLink(token);

        MvcResult result = mockMvc.perform(get(PASSWORD_RESET_ENDPOINT)
                        .param("token", token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        ErrorResponse error = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        assertThat(error.code()).isEqualTo(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        verify(passwordAuthenticationService).validatePasswordResetLink(token);
    }

    @DisplayName("""
            validatePasswordResetLink
            SHOULD return BAD_REQUEST
            WHEN token query parameter is missing
            """)
    @Test
    void validatePasswordResetLink_error1() throws Exception {
        mockMvc.perform(get(PASSWORD_RESET_ENDPOINT))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(passwordAuthenticationService);
    }

    @DisplayName("""
            confirmPasswordReset
            SHOULD return OK
            WITH a password reset success message
            FOR valid request
            """)
    @Test
    void confirmPasswordReset_success() throws Exception {
        ConfirmPasswordResetRequest request = new ConfirmPasswordResetRequest(
                TEST_UUID, CORRECT_PASSWORD, CORRECT_PASSWORD);

        mockMvc.perform(post(CONFIRM_PASSWORD_RESET_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password reset successful"));

        verify(passwordAuthenticationService).confirmPasswordReset(request);
    }

    @DisplayName("""
            confirmPasswordReset
            SHOULD return BAD_REQUEST
            WITH error_code PASSWORD_RESET_TOKEN_INVALID
            FOR invalid token
            """)
    @Test
    void confirmPasswordReset_error() throws Exception {
        ConfirmPasswordResetRequest request = new ConfirmPasswordResetRequest(
                TEST_UUID, CORRECT_PASSWORD, CORRECT_PASSWORD);

        doThrow(new TheraflowApiException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID))
                .when(passwordAuthenticationService).confirmPasswordReset(request);

        MvcResult result = mockMvc.perform(post(CONFIRM_PASSWORD_RESET_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        ErrorResponse error = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        assertThat(error.code()).isEqualTo(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        verify(passwordAuthenticationService).confirmPasswordReset(request);
    }

    @DisplayName("""
            confirmPasswordReset
            SHOULD return BAD_REQUEST
            WITH error_code VALIDATION_FAILED
            FOR mismatched passwords
            """)
    @Test
    void confirmPasswordReset_error1() throws Exception {
        ConfirmPasswordResetRequest request = new ConfirmPasswordResetRequest(
                TEST_UUID, CORRECT_PASSWORD, "DifferentPassword1!");

        MvcResult result = mockMvc.perform(post(CONFIRM_PASSWORD_RESET_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        ErrorResponse error = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        assertThat(error.code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
        assertThat(error.errors()).singleElement().satisfies(validationError -> {
            assertThat(validationError.field()).isEqualTo("confirmPassword");
            assertThat(validationError.code()).isEqualTo(ErrorCode.PASSWORD_NOT_MATCH.name());
        });
        verifyNoInteractions(passwordAuthenticationService);
    }

    @DisplayName("""
            confirmPasswordReset
            SHOULD return BAD_REQUEST
            WITH error_code PASSWORD_WEAK
            FOR weak password
            """)
    @Test
    void confirmPasswordReset_error2() throws Exception {
        ConfirmPasswordResetRequest request = new ConfirmPasswordResetRequest(
                TEST_UUID, "weak", "weak");

        MvcResult result = mockMvc.perform(post(CONFIRM_PASSWORD_RESET_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        ErrorResponse error = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        assertThat(error.code()).isEqualTo(ErrorCode.PASSWORD_WEAK);
        verifyNoInteractions(passwordAuthenticationService);
    }

    @DisplayName("""
            confirmPasswordReset
            SHOULD return BAD_REQUEST
            WITH error_code VALIDATION_FAILED
            FOR blank token
            """)
    @Test
    void confirmPasswordReset_error3() throws Exception {
        ConfirmPasswordResetRequest request = new ConfirmPasswordResetRequest(
                " ", CORRECT_PASSWORD, CORRECT_PASSWORD);

        MvcResult result = mockMvc.perform(post(CONFIRM_PASSWORD_RESET_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        ErrorResponse error = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        assertThat(error.code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
        assertThat(error.errors()).singleElement().satisfies(validationError -> {
            assertThat(validationError.field()).isEqualTo("token");
            assertThat(validationError.code()).isEqualTo(ErrorCode.REQUIRED_FIELD.name());
        });
        verifyNoInteractions(passwordAuthenticationService);
    }

}
