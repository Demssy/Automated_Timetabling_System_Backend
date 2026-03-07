package com.timetable.backend.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Set;

/**
 * Request DTO for promoting an existing user to a Teacher.
 * The user's credentials (email, password, fullName) are preserved as-is.
 * Only teacher-specific fields and the target userId are required.
 *
 * @param userId          ID of the existing user to promote to TEACHER role
 * @param maxDailyHours   maximum working hours per day for this teacher
 * @param colorCode       hex color used to identify this teacher in the timetable UI
 * @param qualifiedStyleIds set of DanceStyle IDs the teacher is qualified to teach
 */
public record CreateTeacherRequest(
    @NotNull(message = "User ID is required")
    Long userId,

    @Min(value = 1, message = "Max daily hours must be at least 1")
    Integer maxDailyHours,

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Invalid color code format")
    String colorCode,

    Set<Long> qualifiedStyleIds
) {}
