package com.timetable.backend.domain.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for ResourceUnavailability entity.
 */
public record ResourceUnavailabilityDTO(
    Long id,
    @NotNull(message = "Teacher ID is required")
    Long teacherId,
    String teacherName, // Read-only
    @NotNull(message = "Timeslot ID is required")
    Long timeslotId,
    String timeslotDescription, // Read-only, e.g., "MONDAY 10:00-11:00"
    String reason
) {}

