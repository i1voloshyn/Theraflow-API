package dev.ivanvoloshyn.theraflowapi.security;

import dev.ivanvoloshyn.theraflowapi.exception.exceptions.PermissionException;
import dev.ivanvoloshyn.theraflowapi.exception.model.ErrorCode;
import dev.ivanvoloshyn.theraflowapi.jwt.JwtAuthTokenProvider;
import dev.ivanvoloshyn.theraflowapi.security.model.TheraflowUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Set<String> PUBLIC_POST_ENDPOINTS = Set.of(
            "/api/v1/auth/password/sign-up",
            "/api/v1/auth/password/sign-in",
            "/api/v1/auth/password-reset",
            "/api/v1/auth/password-reset/confirm",
            "/api/v1/auth/refresh");
    private static final Set<String> PUBLIC_GET_ENDPOINTS = Set.of(
            "/api/v1/auth/verify-email",
            "/api/v1/auth/password-reset");

    private final JwtAuthTokenProvider jwtAuthTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final SecurityErrorHandler securityErrorHandler;

    public static boolean isPublicAuthenticationRequest(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return ("POST".equals(request.getMethod()) && PUBLIC_POST_ENDPOINTS.contains(path))
                || ("GET".equals(request.getMethod()) && PUBLIC_GET_ENDPOINTS.contains(path));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return isPublicAuthenticationRequest(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            authenticate(request);
        } catch (PermissionException e) {
            SecurityContextHolder.clearContext();
            securityErrorHandler.writeError(request, response, e.getErrorCode());
            return;
        } catch (UsernameNotFoundException e) {
            SecurityContextHolder.clearContext();
            securityErrorHandler.writeError(request, response, ErrorCode.TOKEN_INVALID);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        TheraflowUser tokenUser = jwtAuthTokenProvider.extractUserDetails(token);
        TheraflowUser user = userDetailsService.loadUserByAccountId(tokenUser.getAccountId());
        switch (user.getStatus()) {
            case PENDING_VERIFICATION -> throw new PermissionException(ErrorCode.EMAIL_NOT_VERIFIED);
            case DISABLED -> throw new PermissionException(ErrorCode.ACCOUNT_DISABLED);
            case ACTIVE -> { }
        }
        user.eraseCredentials();

        var authentication = UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}
