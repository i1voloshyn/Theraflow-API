package dev.ivanvoloshyn.theraflowapi.guardian;

import dev.ivanvoloshyn.theraflowapi.guardian.dto.EditGuardianRequest;
import dev.ivanvoloshyn.theraflowapi.guardian.dto.GuardianResponse;
import dev.ivanvoloshyn.theraflowapi.guardian.dto.SaveGuardianRequest;
import dev.ivanvoloshyn.theraflowapi.security.model.TheraflowUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/guardians")
public class GuardianController {
    private final GuardianService guardianService;

    @PostMapping
    public ResponseEntity<GuardianResponse> saveGuardian(
            @Valid @RequestBody SaveGuardianRequest request,
            @AuthenticationPrincipal TheraflowUser user) {
        GuardianResponse response = guardianService.saveGuardian(request, user.getAccountId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping
    public ResponseEntity<GuardianResponse> editGuardian(
            @Valid @RequestBody EditGuardianRequest request,
            @AuthenticationPrincipal TheraflowUser user) {
        GuardianResponse response = guardianService.editGuardian(request, user.getAccountId());
        return ResponseEntity.ok(response);
    }
}
