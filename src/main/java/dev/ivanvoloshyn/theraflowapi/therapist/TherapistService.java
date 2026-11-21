package dev.ivanvoloshyn.theraflowapi.therapist;

import dev.ivanvoloshyn.theraflowapi.account.AccountService;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountStatus;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountType;
import dev.ivanvoloshyn.theraflowapi.account.model.Account;
import dev.ivanvoloshyn.theraflowapi.exception.EntityNotFoundException;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.PermissionException;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.TheraflowApiException;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import dev.ivanvoloshyn.theraflowapi.therapist.dto.TherapistRequest;
import dev.ivanvoloshyn.theraflowapi.therapist.dto.TherapistResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TherapistService {
    private static final Set<String> SORTABLE_PROPERTIES = Set.of(
            "id", "firstName", "lastName", "licenseNumber", "professionalTitle");

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

    public Therapist findTherapistByAccountId(UUID accountId) {
        return therapistRepository.findTherapistByAccountId(accountId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.THERAPIST_NOT_FOUND));
    }

    public Page<TherapistResponse> getAll(Pageable pageable) {
        return therapistRepository.findAll(withStableSort(pageable))
                .map(therapistMapper::toTherapistResponse);
    }

    private Pageable withStableSort(Pageable pageable) {
        Sort sort = pageable.getSort();
        for (Sort.Order order : sort) {
            if (!SORTABLE_PROPERTIES.contains(order.getProperty())) {
                throw new TheraflowApiException(ErrorCode.SORT_PROPERTY_INVALID);
            }
        }

        // Future city sorting: first add the database column/relationship and entity mapping.
        // Then allow "city" above (or map it to the actual location property here).
        // Pass that sort to the repository so the database sorts BEFORE pagination;
        // retain the id tie-breaker below when several therapists share the same city.

        // A unique final sort key keeps page boundaries deterministic for unchanged data.
        // For an unsorted request this also supplies the default order: id ascending.
        if (sort.getOrderFor("id")==null) {
            sort = sort.and(Sort.by("id"));
        }

        return pageable.isPaged()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort)
                :Pageable.unpaged(sort);
    }
}
