package dev.ivanvoloshyn.theraflowapi.jwt;

import dev.ivanvoloshyn.theraflowapi.account.dto.AccountStatus;
import dev.ivanvoloshyn.theraflowapi.account.dto.AccountType;
import dev.ivanvoloshyn.theraflowapi.exception.exceptions.PermissionException;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import dev.ivanvoloshyn.theraflowapi.security.model.TheraflowUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthTokenProvider {
    private static final String ACCOUNT_ID_CLAIM = "accountId";
    private static final String ACCOUNT_TYPE_CLAIM = "account_type";
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";

    private final JwtProvider jwtProvider;

    @Value("${app.security.jwt.access.secret}")
    private String secret;
    @Value("${app.security.jwt.access.expiration}")
    private Duration expiration;

    public String generateAccessToken(TheraflowUser user) {
        return jwtProvider.tokenBuilder(expiration, secret)
                .subject(user.getUsername())
                .claim(ACCOUNT_ID_CLAIM, user.getAccountId().toString())
                .claim(ACCOUNT_TYPE_CLAIM, user.getType().name())
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .compact();
    }

    public TheraflowUser extractUserDetails(String token) {
        Claims claims = jwtProvider.extractAllClaims(token, secret);

        try {
            String email = claims.getSubject();
            String accountId = claims.get(ACCOUNT_ID_CLAIM, String.class);
            String accountType = claims.get(ACCOUNT_TYPE_CLAIM, String.class);
            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);

            if (email == null || email.isBlank()
                    || accountId == null || accountType == null
                    || claims.getExpiration() == null
                    || !ACCESS_TOKEN_TYPE.equals(tokenType)) {
                throw new PermissionException(ErrorCode.TOKEN_INVALID);
            }

            return new TheraflowUser(
                    UUID.fromString(accountId),
                    email,
                    null,
                    AccountType.valueOf(accountType),
                    AccountStatus.ACTIVE
            );
        } catch (JwtException | IllegalArgumentException e) {
            throw new PermissionException(ErrorCode.TOKEN_INVALID, e);
        }
    }
}
