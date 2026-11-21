package dev.ivanvoloshyn.theraflowapi.practicelocation;

import dev.ivanvoloshyn.theraflowapi.practicelocation.dto.PracticeLocationRequest;
import dev.ivanvoloshyn.theraflowapi.practicelocation.dto.PracticeLocationResponse;
import dev.ivanvoloshyn.theraflowapi.practicelocation.model.PracticeLocation;
import dev.ivanvoloshyn.theraflowapi.therapist.Therapist;
import dev.ivanvoloshyn.theraflowapi.therapist.TherapistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor  
@Transactional(readOnly = true)
public class PracticeLocationService {
    private final TherapistService therapistService;
    private final PracticeLocationMapper mapper;

    @Transactional
    public PracticeLocationResponse addPracticeLocation(PracticeLocationRequest request, UUID accountId) {
        Therapist therapist = therapistService.findTherapistByAccountId(accountId);
        PracticeLocation location = mapper.toPracticeLocation(request);
        therapist.addLocation(location);

        return mapper.toResponse(location);
    }
}
