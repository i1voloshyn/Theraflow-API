package dev.ivanvoloshyn.theraflowapi.guardian;

import dev.ivanvoloshyn.theraflowapi.account.AccountService;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountType;
import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import dev.ivanvoloshyn.theraflowapi.exception.EntityNotFoundException;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.PermissionException;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.ResourceConflictException;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import dev.ivanvoloshyn.theraflowapi.guardian.dto.EditGuardianRequest;
import dev.ivanvoloshyn.theraflowapi.guardian.dto.GuardianResponse;
import dev.ivanvoloshyn.theraflowapi.guardian.dto.SaveGuardianRequest;
import dev.ivanvoloshyn.theraflowapi.guardian.model.Guardian;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuardianService {
    private final AccountService accountService;
    private final GuardianRepository guardianRepository;
    private final GuardianMapper guardianMapper;

    @Transactional
    public GuardianResponse saveGuardian(SaveGuardianRequest request, UUID accountId) {
        Account account = accountService.findById(accountId);
        if (account.getType() != AccountType.GUARDIAN) {
            throw new PermissionException(ErrorCode.ACCESS_DENIED);
        }
        if (guardianRepository.existsById(accountId)) {
            throw new ResourceConflictException(ErrorCode.GUARDIAN_ALREADY_EXISTS);
        }

        Guardian guardian = new Guardian();
        guardian.setFirstName(request.firstName().trim());
        guardian.setLastName(request.lastName().trim());
        guardian.setAccount(account);

        Guardian savedGuardian = guardianRepository.save(guardian);
        return guardianMapper.toGuardianResponse(savedGuardian);
    }

    @Transactional
    public GuardianResponse editGuardian(EditGuardianRequest request, UUID accountId) {
        Account account = accountService.findById(accountId);
        if (account.getType() != AccountType.GUARDIAN) {
            throw new PermissionException(ErrorCode.ACCESS_DENIED);
        }

        Guardian guardian = guardianRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.GUARDIAN_NOT_FOUND));
        guardian.setFirstName(request.firstName().trim());
        guardian.setLastName(request.lastName().trim());

        return guardianMapper.toGuardianResponse(guardian);
    }
}
