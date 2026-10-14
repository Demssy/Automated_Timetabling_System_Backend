package com.timetable.backend.domain.dto;

import com.timetable.backend.domain.model.AvailabilityRole;

import java.time.LocalDate;
import java.time.LocalTime;

public record ResourceUnavailabilityDTO(
        Long id,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String reason,
        AvailabilityRole availabilityRole
) {
    /** Convenience constructor; a null role means "use the user's default role". */
    public ResourceUnavailabilityDTO(Long id, LocalDate date, LocalTime startTime, LocalTime endTime, String reason) {
        this(id, date, startTime, endTime, reason, null);
    }
}