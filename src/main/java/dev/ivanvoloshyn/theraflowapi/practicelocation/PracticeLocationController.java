package dev.ivanvoloshyn.theraflowapi.practicelocation;

import dev.ivanvoloshyn.theraflowapi.practicelocation.dto.PracticeLocationRequest;
import dev.ivanvoloshyn.theraflowapi.practicelocation.dto.PracticeLocationResponse;
import dev.ivanvoloshyn.theraflowapi.security.model.TheraflowUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/practice-locations")
public class PracticeLocationController {
    private final PracticeLocationService locationService;

    @PostMapping
    public ResponseEntity<PracticeLocationResponse> addLocation(
            @Valid @RequestBody PracticeLocationRequest request,
            @AuthenticationPrincipal TheraflowUser user
    ) {
        var response = locationService.addPracticeLocation(request, user.getAccountId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response); // created() with location header ????
    }

    @GetMapping
    public ResponseEntity<List<PracticeLocationResponse>> getAllLocations(
            @AuthenticationPrincipal TheraflowUser user
    ) {
        var response = locationService.getAll(user.getAccountId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteLocation(
            @RequestParam("id") UUID id
    ) {
        locationService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
