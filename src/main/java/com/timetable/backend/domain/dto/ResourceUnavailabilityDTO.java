package com.timetable.backend.domain.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ResourceUnavailabilityDTO(
        Long id,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String reason
) {}