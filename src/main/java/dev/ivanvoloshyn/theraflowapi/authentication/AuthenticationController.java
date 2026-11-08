package dev.ivanvoloshyn.theraflowapi.authentication;

import dev.ivanvoloshyn.theraflowapi.authentication.dto.SignUpResponse;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.PasswordSignUpRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final PasswordAuthenticationService passwordAuthenticationService;

    @PostMapping("/password/sign-up")
    public ResponseEntity<SignUpResponse> passwordSignUp(
            @Valid @RequestBody PasswordSignUpRequest request) {
        SignUpResponse response = passwordAuthenticationService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
