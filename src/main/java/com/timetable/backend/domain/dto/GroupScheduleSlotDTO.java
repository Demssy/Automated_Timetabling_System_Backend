package com.timetable.backend.domain.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Represents a single scheduled time slot for a group lesson.
 * Built from the group's pinned Lesson timeslots.
 */
public record GroupScheduleSlotDTO(
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    String teacherName
) {}

