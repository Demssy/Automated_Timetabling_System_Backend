package com.timetable.backend.controller;

import com.timetable.backend.domain.dto.AuthenticationRequest;
import com.timetable.backend.domain.dto.AuthenticationResponse;
import com.timetable.backend.domain.dto.RegisterRequest;
import com.timetable.backend.security.JwtService;
import com.timetable.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    
    @Value("${application.security.cookie.secure:true}")
    private boolean cookieSecure;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request) {
        String token = authService.registerStudent(request.email(), request.password(), request.fullName(), request.birthDate());
        // set cookie
        long maxAgeSec = jwtService.getExpirationMs() / 1000L;
        ResponseCookie cookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAgeSec)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).contentType(MediaType.APPLICATION_JSON).body(new AuthenticationResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody AuthenticationRequest request) {
        String token = authService.authenticate(request.email(), request.password());
        long maxAgeSec = jwtService.getExpirationMs() / 1000L;
        ResponseCookie cookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAgeSec)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).contentType(MediaType.APPLICATION_JSON).body(new AuthenticationResponse(token));
    }
}