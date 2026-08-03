package com.timetable.backend.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating or updating a Lesson.
 *
 * <p>Validation rules enforced in {@code LessonService}:
 * <ul>
 *   <li>Group lesson ({@code isPrivate=false}): {@code danceGroupId} must be non-null, {@code studentId} must be null.</li>
 *   <li>Private lesson ({@code isPrivate=true}): {@code studentId} is optional.
 *       If null, the lesson becomes a "template" for the solver to auto-assign a student.</li>
 *   <li>Pinned lesson ({@code isPinned=true}): {@code timeslotId} must be non-null.</li>
 *   <li>If {@code roomId} is null, the first available room is auto-assigned.</li>
 * </ul>
 */
public record CreateLessonRequest(
    Long scheduleId,   // Required for one-time lessons added directly to a concrete schedule
    @NotNull(message = "Teacher ID is required")
    Long teacherId,
    Long danceGroupId,  // Required for group lessons; must be null for private lessons
    Long studentId,     // Optional for private lessons (null = solver template); must be null for group lessons
    @Min(value = 15, message = "Duration must be at least 15 minutes")
    int durationMinutes,
    boolean isPrivate,
    boolean isPinned,
    boolean isActive,
    Long timeslotId, // Optional; required when isPinned = true
    Long roomId      // Optional; auto-assigned if null
) {}
