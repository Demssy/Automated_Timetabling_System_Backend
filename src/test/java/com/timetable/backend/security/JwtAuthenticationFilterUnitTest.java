package com.timetable.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class JwtAuthenticationFilterUnitTest {

    JwtService jwtService;
    UserDetailsService userDetailsService;
    JwtAuthenticationFilter filter;

    @BeforeEach
    void setup() {
        jwtService = mock(JwtService.class);
        userDetailsService = mock(UserDetailsService.class);
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    @Test
    void filterSetsSecurityContextWhenTokenValid() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(req.getHeader("Authorization")).thenReturn("Bearer faketoken");
        when(jwtService.extractUsername("faketoken")).thenReturn("user@example.com");

        UserDetails ud = User.withUsername("user@example.com").password("pw").authorities("ROLE_USER").build();
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(ud);
        when(jwtService.isTokenValid("faketoken", ud)).thenReturn(true);

        filter.doFilterInternal(req, res, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("user@example.com");

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

        when(jwtService.extractUsername("cookietoken")).thenReturn("cookieuser@example.com");

        UserDetails ud = User.withUsername("cookieuser@example.com").password("pw").authorities("ROLE_USER").build();
        when(userDetailsService.loadUserByUsername("cookieuser@example.com")).thenReturn(ud);
        when(jwtService.isTokenValid("cookietoken", ud)).thenReturn(true);

        filter.doFilterInternal(req, res, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("cookieuser@example.com");

        SecurityContextHolder.clearContext();
    }
}
