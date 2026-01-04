package com.timetable.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Stateless JWT Authentication Filter.
 * Does NOT query the database - extracts user identity and roles directly from the JWT token.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Extract JWT from Authorization header or cookie
        var jwt = extractJwtFromRequest(request);

        // If no token present, skip authentication
        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Validate token and populate SecurityContext (Stateless - no DB call)
        try {
            if (jwtService.isTokenValid(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
                var username = jwtService.extractUsername(jwt);
                var authorities = jwtService.extractAuthorities(jwt);

                var authToken = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        authorities
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            // Log and continue without authentication if token is invalid
            logger.debug("Invalid JWT token: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts JWT token from Authorization header (Bearer) or jwt cookie.
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Fallback: try cookie named 'jwt'
        return extractTokenFromCookie(request);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (var c : request.getCookies()) {
            if ("jwt".equals(c.getName())) return c.getValue();
        }
        return null;
    }
}
