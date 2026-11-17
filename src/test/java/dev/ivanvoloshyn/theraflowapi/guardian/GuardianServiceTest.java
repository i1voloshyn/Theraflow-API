package dev.ivanvoloshyn.theraflowapi.guardian;

import dev.ivanvoloshyn.theraflowapi.account.AccountService;
import dev.ivanvoloshyn.theraflowapi.account.AccountTestBuilder;
import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import dev.ivanvoloshyn.theraflowapi.exception.EntityNotFoundException;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.PermissionException;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import dev.ivanvoloshyn.theraflowapi.guardian.dto.GuardianResponse;
import dev.ivanvoloshyn.theraflowapi.guardian.dto.SaveGuardianRequest;
import dev.ivanvoloshyn.theraflowapi.guardian.model.Guardian;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static dev.ivanvoloshyn.theraflowapi.fixtures.Constants.ACCOUNT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardianServiceTest {
    @Mock
    private AccountService accountService;
    @Mock
    private GuardianRepository guardianRepository;
    @Spy
    private GuardianMapper guardianMapper = Mappers.getMapper(GuardianMapper.class);
    @InjectMocks
    private GuardianService guardianService;

    @DisplayName("""
            saveGuardian
            SHOULD save guardian with trimmed names and return GuardianResponse
            WHEN account type is GUARDIAN
            """)
    @Test
    void saveGuardian_success() {
        SaveGuardianRequest request = GuardianTestBuilder.saveGuardianRequestWithSpaces();
        Account account = AccountTestBuilder.guardianVerifiedAccount();
        Guardian expectedGuardian = GuardianTestBuilder.savedGuardian(account);

        when(accountService.findById(ACCOUNT_ID)).thenReturn(account);
        when(guardianRepository.existsById(ACCOUNT_ID)).thenReturn(false);
        when(guardianRepository.save(any(Guardian.class))).thenReturn(expectedGuardian);

        GuardianResponse actualResponse = guardianService.saveGuardian(request, ACCOUNT_ID);

        verify(accountService).findById(ACCOUNT_ID);
        verify(guardianRepository).existsById(ACCOUNT_ID);
        ArgumentCaptor<Guardian> guardianCaptor = ArgumentCaptor.forClass(Guardian.class);
        verify(guardianRepository).save(guardianCaptor.capture());
        verify(guardianMapper).toGuardianResponse(expectedGuardian);
        Guardian guardian = guardianCaptor.getValue();

        assertThat(guardian.getFirstName()).isEqualTo(expectedGuardian.getFirstName());
        assertThat(guardian.getLastName()).isEqualTo(expectedGuardian.getLastName());
        assertThat(guardian.getAccount()).isSameAs(account);
        assertThat(actualResponse.id()).isEqualTo(expectedGuardian.getId());
        assertThat(actualResponse.firstName()).isEqualTo(expectedGuardian.getFirstName());
        assertThat(actualResponse.lastName()).isEqualTo(expectedGuardian.getLastName());
    }

    @DisplayName("""
            saveGuardian
            SHOULD throw PermissionException with ACCESS_DENIED
            WHEN account type is THERAPIST
            """)
    @Test
    void saveGuardian_error() {
        SaveGuardianRequest request = GuardianTestBuilder.defaultSaveGuardianRequest();
        Account account = AccountTestBuilder.therapistVerifiedAccount();

        when(accountService.findById(ACCOUNT_ID)).thenReturn(account);

        assertThatExceptionOfType(PermissionException.class)
                .isThrownBy(() -> guardianService.saveGuardian(request, ACCOUNT_ID))
                .matches(ex -> ex.getErrorCode().equals(ErrorCode.ACCESS_DENIED));

        verifyNoInteractions(guardianRepository, guardianMapper);
    }

    @DisplayName("""
            saveGuardian
            SHOULD throw EntityNotFoundException with ACCOUNT_NOT_FOUND
            WHEN account does not exist
            """)
    @Test
    void saveGuardian_error1() {
        SaveGuardianRequest request = GuardianTestBuilder.defaultSaveGuardianRequest();

        when(accountService.findById(ACCOUNT_ID))
                .thenThrow(new EntityNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));

        assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> guardianService.saveGuardian(request, ACCOUNT_ID))
                .matches(ex -> ex.getErrorCode().equals(ErrorCode.ACCOUNT_NOT_FOUND));

        verifyNoInteractions(guardianRepository, guardianMapper);
    }
}
