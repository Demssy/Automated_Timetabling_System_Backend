package com.timetable.backend.controller;

import com.timetable.backend.domain.dto.ResourceUnavailabilityDTO;
import com.timetable.backend.service.ResourceUnavailabilityService;
import com.timetable.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/unavailability")
@RequiredArgsConstructor
public class ResourceUnavailabilityController {

    private final ResourceUnavailabilityService service;
    private final UserService userService;

    /**
     * Get all one-time exceptions for a specific user.
     * Can be accessed by ADMIN or the user themselves (if you add security checks).
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<ResourceUnavailabilityDTO>> getByUserId(
            @PathVariable Long userId,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            Long currentUserId = userService.getCurrentUserInfo(authentication.getName()).id();
            if (!userId.equals(currentUserId)) {
                throw new AccessDeniedException("Access denied for requested user availability");
            }
        }

        return ResponseEntity.ok(service.getByUserId(userId));
    }

    /**
     * Bulk update one-time exceptions for a specific user.
     * Replaces the entire list of exceptions for this user.
     */
    @PutMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateUserExceptions(
            @PathVariable Long userId,
            @RequestBody @Valid List<ResourceUnavailabilityDTO> dtos) {

        service.updateUserExceptions(userId, dtos);
        return ResponseEntity.ok().build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}