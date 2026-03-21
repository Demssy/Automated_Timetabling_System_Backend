package com.timetable.backend.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating or updating a Lesson.
 */
public record CreateLessonRequest(
    @NotNull(message = "Teacher ID is required")
    Long teacherId,
    @NotNull(message = "Dance Group ID is required")
    Long danceGroupId,
    @Min(value = 15, message = "Duration must be at least 15 minutes")
    int durationMinutes,
    boolean isPrivate,
    boolean isPinned,
    boolean isActive,
    Long timeslotId, // Optional, can be null
    Long roomId      // Optional, can be null
) {}

