package com.timetable.backend.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Request DTO for creating or updating a single weekly availability slot.
 * Used by the admin panel to manage granular time windows for teachers and students.
 *
 * @param dayOfWeek day of the week for this availability window
 * @param startTime start of the available window (inclusive)
 * @param endTime   end of the available window (exclusive)
 */
public record WeeklyAvailabilityRequest(
        @NotNull(message = "Day of week is required")
        DayOfWeek dayOfWeek,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "End time is required")
        LocalTime endTime
) {}

