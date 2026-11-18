package dev.ivanvoloshyn.theraflowapi.therapist;

import dev.ivanvoloshyn.theraflowapi.security.model.TheraflowUser;
import dev.ivanvoloshyn.theraflowapi.therapist.dto.TherapistRequest;
import dev.ivanvoloshyn.theraflowapi.therapist.dto.TherapistResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/therapists")
public class TherapistController {
    private final TherapistService therapistService;

    @PostMapping
    public ResponseEntity<TherapistResponse> saveTherapist(
            @Valid @RequestBody TherapistRequest request,
            @AuthenticationPrincipal TheraflowUser user) {
        TherapistResponse response = therapistService.saveProfile(request, user.getAccountId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public PagedModel<TherapistResponse> findAll(
            @PageableDefault() Pageable pageable) {
        return new PagedModel<>(therapistService.getAll(pageable));
    }
}
