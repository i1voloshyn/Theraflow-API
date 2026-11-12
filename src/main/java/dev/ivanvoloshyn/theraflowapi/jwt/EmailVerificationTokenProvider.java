package dev.ivanvoloshyn.theraflowapi.jwt;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationTokenProvider {
    private final JwtProvider jwtProvider;

    @Value("${app.security.jwt.email.secret}")
    private String secret;
    @Value("${app.security.jwt.email.expiration}")
    private Duration expiration;

    public String generateToken(String email) {
        return jwtProvider
                .tokenBuilder(expiration, secret)
                .subject(email)
                .compact();
    }

    public String extractEmail(String token) {
        return jwtProvider.extractClaim(token, secret, Claims::getSubject);
    }
}
