package com.timetable.backend.domain.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;


public record WeeklyAvailabilityDTO(
        Long id,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {}