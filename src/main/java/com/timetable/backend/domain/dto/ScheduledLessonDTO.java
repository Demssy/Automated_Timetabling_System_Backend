package com.timetable.backend.domain.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * DTO representing a scheduled lesson with all assignments.
 *
 * @param lessonId lesson identifier
 * @param teacherName teacher's full name
 * @param groupName dance group name
 * @param dayOfWeek assigned day
 * @param startTime assigned start time
 * @param endTime assigned end time
 * @param roomName assigned room name
 * @param durationMinutes lesson duration
 * @param isPrivate whether this is a private lesson
 * @param isPinned whether this lesson was pinned (not changed by solver)
 */
public record ScheduledLessonDTO(
    Long lessonId,
    String teacherName,
    String groupName,
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    String roomName,
    int durationMinutes,
    boolean isPrivate,
    boolean isPinned
) {
}

