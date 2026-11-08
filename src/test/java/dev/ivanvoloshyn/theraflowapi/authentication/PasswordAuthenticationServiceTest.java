package dev.ivanvoloshyn.theraflowapi.authentication;

import dev.ivanvoloshyn.theraflowapi.account.AccountService;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.PasswordSignUpRequest;
import dev.ivanvoloshyn.theraflowapi.authentication.model.AuthProvider;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.ResourceConflictException;
import dev.ivanvoloshyn.theraflowapi.fixtures.SignUpRequestTestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordAuthenticationServiceTest {
    @Mock
    private AuthIdentityRepository authIdentityRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AccountService accountService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @InjectMocks
    private PasswordAuthenticationService authenticationService;

    @DisplayName("""
            signUp
            SHOULD throw ResourceConflictException
            WHEN provided email with AuthProvider already exists in DB
            """)
    @Test
    void signUp_1() {
        PasswordSignUpRequest request = SignUpRequestTestBuilder.defaultTherapistSignUpRequest();

        when(authIdentityRepository.existsByProviderAndProviderUserId(AuthProvider.PASSWORD, request.email()))
                .thenReturn(true);

        assertThatExceptionOfType(ResourceConflictException.class)
                .isThrownBy(() -> authenticationService.signUp(request))
                .matches(ex -> ex.getErrorCode().equals(ErrorCode.EMAIL_ALREADY_EXISTS));

        verifyNoInteractions(passwordEncoder, accountService, applicationEventPublisher);

    }

}