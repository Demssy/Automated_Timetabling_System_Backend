package com.timetable.backend.domain.dto;

import java.util.Set;

public record TeacherResponse(
    Long id,
    String email,
    String fullName,
    Integer maxDailyHours,
    String colorCode,
    Set<DanceStyleDTO> qualifiedStyles
) {}
