package com.timetable.backend.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing a scheduled lesson with all assignments.
 * Matches the Frontend ScheduledLessonDTO interface exactly.
 *
 * <p>For group lessons: {@code danceGroup} is non-null, {@code student} is null.
 * <p>For private lessons: {@code student} is non-null, {@code danceGroup} is null.
 */
public record ScheduledLessonDTO(
    Long id,
    TeacherResponse teacher,
    DanceGroupDTO danceGroup,   // null for private lessons
    StudentResponse student,    // null for group lessons
    int durationMinutes,
    @JsonProperty("isPrivate") boolean isPrivate,
    @JsonProperty("isPinned") boolean isPinned,
    @JsonProperty("isActive") boolean isActive,
    TimeslotDTO timeslot,
    RoomDTO room
) {
}

