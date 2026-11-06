package dev.ivanvoloshyn.theraflowapi.authentication;

import dev.ivanvoloshyn.theraflowapi.authentication.dto.PasswordSignUpRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public void passwordSignUp(
            @Valid @RequestBody PasswordSignUpRequest request) {
        passwordAuthenticationService.signUp(request);
        System.out.println("OK");
    }
}
