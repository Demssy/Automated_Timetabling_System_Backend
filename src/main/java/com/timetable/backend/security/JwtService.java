package com.timetable.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtService {

    @Value("${jwt.secret:ZmFrZV9zZWNyZXRfa2V5X2Zvcl9kZXY=}")
    private String secret; // base64 encoded

    // expose expiration for cookie max-age calculation
    @Getter
    @Value("${jwt.expiration-ms:3600000}")
    private long expirationMs;

    private Key getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (Exception ex) {
            keyBytes = secret.getBytes();
        }

        // If provided key material is too short for HS256, derive a 256-bit key via SHA-256
        if (keyBytes.length < 32) {
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                keyBytes = digest.digest(keyBytes);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UserDetails userDetails) {
        // Extract roles/authorities from UserDetails and embed them in JWT claims
        var authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", authorities);

        return generateToken(claims, userDetails.getUsername());
    }

    public String generateToken(Map<String, Object> extraClaims, String subject) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts authorities/roles from the JWT token.
     * This allows stateless authentication without hitting the database.
     *
     * @param token the JWT token
     * @return collection of granted authorities
     */
    @SuppressWarnings("unchecked")
    public Collection<? extends GrantedAuthority> extractAuthorities(String token) {
        var claims = extractAllClaims(token);
        var authoritiesList = (List<String>) claims.get("authorities");

        if (authoritiesList == null || authoritiesList.isEmpty()) {
            return List.of(); // return empty list if no authorities present
        }

        return authoritiesList.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    /**
     * Validates token (signature and expiration only).
     * Stateless - does NOT require database lookup.
     *
     * @param token the JWT token
     * @return true if token is valid
     */
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Legacy method - validates token against UserDetails.
     * For stateless mode, prefer {@link #isTokenValid(String)}.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
