package com.timetable.backend.domain.dto;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * DTO for Timeslot entity.
 *
 * @param id timeslot identifier
 * @param dayOfWeek day of the week
 * @param startTime start time of the slot
 * @param endTime end time of the slot
 */
public record TimeslotDTO(
    Long id,
    @NotNull(message = "Day of week is required")
    DayOfWeek dayOfWeek,
    @NotNull(message = "Start time is required")
    LocalTime startTime,
    @NotNull(message = "End time is required")
    LocalTime endTime
) {}

