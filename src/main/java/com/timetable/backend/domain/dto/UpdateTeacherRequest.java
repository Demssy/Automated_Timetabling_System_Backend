package com.timetable.backend.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Set;

/**
 * DTO for updating teacher profile information.
 * Excludes email and password (email is immutable, password is changed via separate endpoint).
 *
 * @param fullName teacher's full name
 * @param maxDailyHours maximum hours per day the teacher can work
 * @param colorCode color code for UI representation (hex format)
 * @param qualifiedStyleIds set of dance style IDs the teacher is qualified to teach (null = no change)
 */
public record UpdateTeacherRequest(
    @NotBlank(message = "Full name is required")
    String fullName,

    @Min(value = 1, message = "Max daily hours must be at least 1")
    Integer maxDailyHours,

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Invalid color code format")
    String colorCode,

    Set<Long> qualifiedStyleIds  // Can be null if not updating dance styles
) {}

