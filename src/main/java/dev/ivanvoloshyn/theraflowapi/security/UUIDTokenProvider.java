package dev.ivanvoloshyn.theraflowapi.security;

import dev.ivanvoloshyn.theraflowapi.authentication.model.PasswordResetToken;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@RequiredArgsConstructor
@Component
public class UUIDTokenProvider {
    private final Clock clock;

    @Value("${app.password.reset.expiration}")
    private Duration passwordResetTokenExpiration;

    public PasswordResetToken buildPasswordResetToken(String rawToken) {
        String tokenHash = hashToken(rawToken);

        return new PasswordResetToken(tokenHash, getExpiration(passwordResetTokenExpiration));
    }

    private Instant getExpiration(Duration expiredIn) {
        Instant now = clock.instant();
        return now.plus(expiredIn);
    }


    public String hashToken(String token) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = messageDigest.digest(token.getBytes(StandardCharsets.UTF_8));
            return new String(Hex.encode(bytes));
        } catch (Exception e) {
            throw new RuntimeException("Error hashing refresh tokens", e);
        }
    }
}
