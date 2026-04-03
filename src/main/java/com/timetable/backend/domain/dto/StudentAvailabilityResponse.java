package com.timetable.backend.domain.dto;

import java.util.List;

/**
 * Response DTO for a student's weekly availability schedule.
 * Returned by GET /api/teachers/me/students/{studentId}/availability
 */
public record StudentAvailabilityResponse(
        List<WeeklyAvailabilityDTO> weeklyAvailabilities
) {}

