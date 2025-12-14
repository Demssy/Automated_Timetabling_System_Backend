package com.timetable.backend.security;

import com.timetable.backend.security.dto.AuthenticationRequest;
import com.timetable.backend.security.dto.AuthenticationResponse;
import com.timetable.backend.security.dto.RegisterRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request) {
        String token = authService.registerStudent(request.email(), request.password(), request.fullName(), request.birthDate());
        // set cookie
        long maxAgeSec = jwtService.getExpirationMs() / 1000L;
        ResponseCookie cookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(false) // set true in prod
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
                .secure(false)
                .path("/")
                .maxAge(maxAgeSec)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).contentType(MediaType.APPLICATION_JSON).body(new AuthenticationResponse(token));
    }
}
