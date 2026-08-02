package com.timetable.backend.controller;

import com.timetable.backend.domain.dto.AuthenticationRequest;
import com.timetable.backend.domain.dto.RegisterRequest;
import com.timetable.backend.domain.dto.UserResponse;
import com.timetable.backend.security.JwtService;
import com.timetable.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    
    @Value("${application.security.cookie.secure:false}")
    private boolean cookieSecure;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse userResponse = authService.register(request);

        var userDetails = org.springframework.security.core.userdetails.User.withUsername(userResponse.email())
                .password("")
                .roles(userResponse.role())
                .build();
        String token = jwtService.generateToken(userDetails);

        return getUserResponseWithCookie(token, userResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestBody AuthenticationRequest request) {
        UserResponse userResponse = authService.authenticate(request.email(), request.password());

        // Generate token for the authenticated user
        var userDetails = User.withUsername(userResponse.email())
                .password("")
                .roles(userResponse.role())
                .build();
        String token = jwtService.generateToken(userDetails);

        return getUserResponseWithCookie(token, userResponse);
    }

    @RequestMapping(value = "/logout", method = {GET, POST})
    public ResponseEntity<Void> logout() {

        // Clear JWT cookie by setting maxAge to 0
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Lax") // Changed from "Strict" to "Lax" for stable HTTP proxying
                // REMOVED .domain("localhost") to allow dynamic domain binding
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @NonNull
    private ResponseEntity<UserResponse> getUserResponseWithCookie(String token, UserResponse userResponse) {
        long maxAgeSec = jwtService.getExpirationMs() / 1000L;
        ResponseCookie cookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAgeSec)
                .sameSite("Lax") // Changed from "Strict" to "Lax"
                // REMOVED .domain("localhost") to make the build environment-agnostic
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(userResponse);
    }
}