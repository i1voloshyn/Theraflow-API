package dev.ivanvoloshyn.theraflowapi.jwt;


import dev.ivanvoloshyn.theraflowapi.exception.exceptions.PermissionException;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtProvider {
    private final Clock clock;

    public JwtBuilder tokenBuilder(
            Duration jwtExpiration,
            String secret
    ) {
        Instant now = clock.instant();
        Instant expiration = now.plus(jwtExpiration);
        return Jwts.builder()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSignInKey(secret));
    }

    public Claims extractAllClaims(String token, String secret) {
        try {
            Instant now = clock.instant();
            Claims claims = Jwts.parser()
                    .verifyWith((SecretKey) getSignInKey(secret))
                    .clock(() -> Date.from(now))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Date expiration = claims.getExpiration();
            // RFC 7519 requires the current time to be strictly before exp.
            if (expiration != null && !expiration.toInstant().isAfter(now)) {
                throw new PermissionException(ErrorCode.TOKEN_EXPIRED);
            }
            return claims;
        } catch (ExpiredJwtException e) {
            log.debug("Token signature is valid, but the access has expired.", e);
            throw new PermissionException(ErrorCode.TOKEN_EXPIRED, e);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token is completely invalid, structural failure, or tampered signature.", e);
            throw new PermissionException(ErrorCode.TOKEN_INVALID, e);
        }
    }

    public <V> V extractClaim(String token, String secret, Function<Claims, V> claimResolver) {
        Claims claims = extractAllClaims(token, secret);
        return claimResolver.apply(claims);
    }

    private Key getSignInKey(String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
