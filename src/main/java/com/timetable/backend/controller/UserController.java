package com.timetable.backend.controller;

import com.timetable.backend.domain.dto.UserResponse;
import com.timetable.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authenticated user operations.
 * Provides endpoints for retrieving current user information.
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Get information about the currently authenticated user.
     * Extracts the username (email) from the Spring Security Authentication object.
     *
     * @param authentication Spring Security authentication object (injected automatically)
     * @return ResponseEntity containing UserResponse with current user details
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        String email = authentication.getName(); // username is email in our case
        UserResponse userResponse = userService.getCurrentUserInfo(email);
        return ResponseEntity.ok(userResponse);
    }
}

