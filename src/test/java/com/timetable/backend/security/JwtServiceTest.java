package com.timetable.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class JwtServiceTest {

    @Autowired
    JwtService jwtService;

    @Test
    void generateAndValidateToken() {
        UserDetails user = User.withUsername("user@example.com").password("password").authorities("ROLE_USER").build();
        String token = jwtService.generateToken(user);

        String username = jwtService.extractUsername(token);
        assertThat(username).isEqualTo("user@example.com");

        boolean valid = jwtService.isTokenValid(token, user);
        assertThat(valid).isTrue();
    }

    @Test
    void expiredTokenIsInvalid() {
        // Build an isolated JwtService with a negative TTL to issue an already-expired token
        JwtService expiredTokenIssuer = new JwtService();
        ReflectionTestUtils.setField(expiredTokenIssuer, "secret",
                ReflectionTestUtils.getField(jwtService, "secret"));
        ReflectionTestUtils.setField(expiredTokenIssuer, "expirationMs", -1_000L);

        UserDetails user = User.withUsername("u2@example.com").password("password").authorities("ROLE_USER").build();
        String expiredToken = expiredTokenIssuer.generateToken(user);

        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
    }
}

