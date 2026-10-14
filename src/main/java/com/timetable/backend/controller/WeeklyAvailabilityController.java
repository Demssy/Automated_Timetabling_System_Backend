package com.timetable.backend.controller;

import com.timetable.backend.domain.dto.WeeklyAvailabilityDTO;
import com.timetable.backend.domain.dto.WeeklyAvailabilityRequest;
import com.timetable.backend.domain.model.AvailabilityRole;
import com.timetable.backend.service.UserService;
import com.timetable.backend.service.WeeklyAvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Weekly availability of the current user, scoped by availability role so that
 * dual-role users (teacher + student) can maintain two independent schedules.
 */
@RestController
@RequestMapping("/api/availability/weekly")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
public class WeeklyAvailabilityController {

    private final WeeklyAvailabilityService weeklyAvailabilityService;
    private final UserService userService;

    /** GET /api/availability/weekly?role=TEACHER|STUDENT (default TEACHER). */
    @GetMapping
    public ResponseEntity<List<WeeklyAvailabilityDTO>> getMyWeeklyAvailability(
            @RequestParam(defaultValue = "TEACHER") AvailabilityRole role,
            Authentication authentication) {
        Long userId = userService.getCurrentUserInfo(authentication.getName()).id();
        return ResponseEntity.ok(weeklyAvailabilityService.getByUserId(userId, role));
    }

    /**
     * POST /api/availability/weekly - creates a slot; availabilityRole is taken from the payload
     * (default role of the user when omitted) and validated against the user's roles.
     */
    @PostMapping
    public ResponseEntity<WeeklyAvailabilityDTO> createMyWeeklySlot(
            @RequestBody @Valid WeeklyAvailabilityRequest request,
            Authentication authentication) {
        Long userId = userService.getCurrentUserInfo(authentication.getName()).id();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(weeklyAvailabilityService.createSlot(userId, request));
    }
}