package com.timetable.backend.domain.dto;

import com.timetable.backend.domain.model.AvailabilityRole;

import java.time.DayOfWeek;
import java.time.LocalTime;


public record WeeklyAvailabilityDTO(
        Long id,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        AvailabilityRole availabilityRole
) {
    /** Convenience constructor; a null role means "use the user's default role". */
    public WeeklyAvailabilityDTO(Long id, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this(id, dayOfWeek, startTime, endTime, null);
    }
}