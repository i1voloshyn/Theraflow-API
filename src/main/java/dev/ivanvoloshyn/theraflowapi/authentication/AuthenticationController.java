package dev.ivanvoloshyn.theraflowapi.authentication;

import dev.ivanvoloshyn.theraflowapi.authentication.dto.ConfirmPasswordResetRequest;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.JwtPair;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.PasswordResetRequest;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.PasswordSignInRequest;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.SignUpResponse;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.PasswordSignUpRequest;
import dev.ivanvoloshyn.theraflowapi.authentication.dto.TokenRotateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @PostMapping("/password/sign-in")
    public ResponseEntity<JwtPair> passwordSignIn(
            @Valid @RequestBody PasswordSignInRequest request) {
        JwtPair response = passwordAuthenticationService.signIn(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtPair> rotateTokens(
            @Valid @RequestBody TokenRotateRequest request) {
        JwtPair response = passwordAuthenticationService.rotateTokens(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(
            @RequestParam("token") String token
    ) {
        passwordAuthenticationService.verifyEmail(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset")
    public ResponseEntity<String> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        passwordAuthenticationService.requestPasswordReset(request.email());
        return ResponseEntity.ok().body("If the email is registered, you'll get a reset link");
    }

    @GetMapping("/password-reset")
    public ResponseEntity<Void> validatePasswordResetLink(
            @RequestParam("token") String token
    ) {
        passwordAuthenticationService.validatePasswordResetLink(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<String> confirmPasswordReset(
            @Valid @RequestBody ConfirmPasswordResetRequest request
    ) {
        passwordAuthenticationService.confirmPasswordReset(request);
        return ResponseEntity.ok().body("Password reset successful");
    }
}
