package com.timetable.backend.domain.dto;

import java.util.Set;

public record CreateTeacherRequest(
    String email,
    String password,
    String fullName,
    Integer maxDailyHours,
    String colorCode,
    Set<Long> qualifiedStyleIds
) {}
