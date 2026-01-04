package com.timetable.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class JwtAuthenticationFilterUnitTest {

    JwtService jwtService;
    JwtAuthenticationFilter filter;

    @BeforeEach
    void setup() {
        jwtService = mock(JwtService.class);
        filter = new JwtAuthenticationFilter(jwtService);
    }

    @Test
    void filterSetsSecurityContextWhenTokenValid() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getHeader("Authorization")).thenReturn("Bearer faketoken");

        // Mock stateless JWT service methods
        when(jwtService.isTokenValid("faketoken")).thenReturn(true);
        when(jwtService.extractUsername("faketoken")).thenReturn("user@example.com");

        Collection<? extends GrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        doReturn(authorities).when(jwtService).extractAuthorities("faketoken");

        filter.doFilterInternal(req, res, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("user@example.com");
        assertThat(auth.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");

        // cleanup
        SecurityContextHolder.clearContext();
    }

    @Test
    void filterSkipsWhenNoHeader() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(req, res, chain);

        verify(chain).doFilter(req, res);
    }

    @Test
    void filterReadsTokenFromCookieWhenNoAuthorizationHeader() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getHeader("Authorization")).thenReturn(null);
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwt", "cookietoken");
        when(req.getCookies()).thenReturn(new jakarta.servlet.http.Cookie[]{cookie});

        // Mock stateless JWT service methods
        when(jwtService.isTokenValid("cookietoken")).thenReturn(true);
        when(jwtService.extractUsername("cookietoken")).thenReturn("cookieuser@example.com");

        Collection<? extends GrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        doReturn(authorities).when(jwtService).extractAuthorities("cookietoken");

        filter.doFilterInternal(req, res, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("cookieuser@example.com");
        assertThat(auth.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");

        SecurityContextHolder.clearContext();
    }
}
