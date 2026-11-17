package dev.ivanvoloshyn.theraflowapi.therapist;

import dev.ivanvoloshyn.theraflowapi.account.AccountService;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountStatus;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountType;
import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.PermissionException;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import dev.ivanvoloshyn.theraflowapi.therapist.dto.TherapistRequest;
import dev.ivanvoloshyn.theraflowapi.therapist.dto.TherapistResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TherapistService {

    private final AccountService accountService;
    private final TherapistMapper therapistMapper;
    private final TherapistRepository therapistRepository;

    @Transactional
    public TherapistResponse saveProfile(TherapistRequest request, UUID accountId) {
        Account account = accountService.findById(accountId);
        if (!account.getStatus().equals(AccountStatus.ACTIVE) || account.getType()!=AccountType.THERAPIST) {
            throw new PermissionException(ErrorCode.ACCESS_DENIED);
        }

        TherapistRequest trimmedRequest = new TherapistRequest(
                request.firstName().trim(),
                request.lastName().trim(),
                request.licenseNumber().trim(),
                request.professionalTitle().trim()
        );
        Therapist therapistProfile = therapistMapper.toTherapist(trimmedRequest);
        therapistProfile.setAccount(account);
        Therapist savedTherapist = therapistRepository.save(therapistProfile);

        return therapistMapper.toTherapistResponse(savedTherapist);
    }
}
