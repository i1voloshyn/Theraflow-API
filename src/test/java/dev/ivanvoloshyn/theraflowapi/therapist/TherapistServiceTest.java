package dev.ivanvoloshyn.theraflowapi.therapist;

import dev.ivanvoloshyn.theraflowapi.account.AccountService;
import dev.ivanvoloshyn.theraflowapi.account.AccountTestBuilder;
import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.PermissionException;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import dev.ivanvoloshyn.theraflowapi.therapist.dto.TherapistRequest;
import dev.ivanvoloshyn.theraflowapi.therapist.dto.TherapistResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.ACCOUNT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TherapistServiceTest {
    @Mock
    private AccountService accountService;
    @Spy
    private TherapistMapper therapistMapper = Mappers.getMapper(TherapistMapper.class);
    @Mock
    private TherapistRepository therapistRepository;
    @InjectMocks
    private TherapistService therapistService;

    @DisplayName("""
            saveProfile
            SHOULD trim all fields, save a therapist linked to the account
            AND return TherapistResponse
            WHEN the account is an active therapist
            """)
    @Test
    void saveProfile_success() {
        TherapistRequest request = TherapistTestBuilder.therapistRequestWithWhitespace();
        Account account = AccountTestBuilder.therapistVerifiedAccount();

        when(accountService.findById(ACCOUNT_ID)).thenReturn(account);

        when(therapistRepository.save(any(Therapist.class))).thenAnswer(invocation -> {
            Therapist therapist = invocation.getArgument(0);
            ReflectionTestUtils.setField(therapist, "id", ACCOUNT_ID);
            return therapist;
        });

        TherapistResponse actualResponse = therapistService.saveProfile(request, ACCOUNT_ID);

        ArgumentCaptor<Therapist> therapistCaptor = ArgumentCaptor.forClass(Therapist.class);
        verify(accountService).findById(ACCOUNT_ID);
        verify(therapistRepository).save(therapistCaptor.capture());

        Therapist savedTherapist = therapistCaptor.getValue();
        assertThat(savedTherapist.getFirstName()).isEqualTo("Joe");
        assertThat(savedTherapist.getLastName()).isEqualTo("Toronto");
        assertThat(savedTherapist.getLicenseNumber()).isEqualTo("LICENSE-123");
        assertThat(savedTherapist.getProfessionalTitle()).isEqualTo("Therapist");
        assertThat(savedTherapist.getAccount()).isSameAs(account);
        assertThat(actualResponse).isEqualTo(new TherapistResponse(
                ACCOUNT_ID, "Joe", "Toronto", "LICENSE-123", "Therapist"));
    }

    @DisplayName("""
            saveProfile
            SHOULD throw PermissionException with ACCESS_DENIED
            WHEN the therapist account is not active
            """)
    @Test
    void saveProfile_error1() {
        TherapistRequest request = TherapistTestBuilder.defaultTherapistRequest();
        Account account = AccountTestBuilder.therapistUnverifiedAccount();

        when(accountService.findById(ACCOUNT_ID)).thenReturn(account);

        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> therapistService.saveProfile(request, ACCOUNT_ID))
                .matches(ex -> ex.getErrorCode().equals(ErrorCode.ACCESS_DENIED));

        verify(accountService).findById(ACCOUNT_ID);
        verifyNoInteractions(therapistMapper, therapistRepository);
    }

    @DisplayName("""
            saveProfile
            SHOULD throw PermissionException with ACCESS_DENIED
            WHEN the active account is not a therapist
            """)
    @Test
    void saveProfile_error2() {
        TherapistRequest request = TherapistTestBuilder.defaultTherapistRequest();
        Account account = AccountTestBuilder.guardianVerifiedAccount();

        when(accountService.findById(ACCOUNT_ID)).thenReturn(account);

        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> therapistService.saveProfile(request, ACCOUNT_ID))
                .matches(ex -> ex.getErrorCode().equals(ErrorCode.ACCESS_DENIED));

        verify(accountService).findById(ACCOUNT_ID);
        verifyNoInteractions(therapistMapper, therapistRepository);
    }
}
