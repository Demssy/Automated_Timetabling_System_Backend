package com.timetable.backend.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing a scheduled lesson with all assignments.
 * Matches the Frontend ScheduledLessonDTO interface exactly.
 */
public record ScheduledLessonDTO(
    Long id,
    TeacherResponse teacher,
    DanceGroupDTO danceGroup,
    int durationMinutes,
    @JsonProperty("isPrivate") boolean isPrivate,
    @JsonProperty("isPinned") boolean isPinned,
    TimeslotDTO timeslot,
    RoomDTO room
) {
}

