package com.timetable.backend.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for updating an existing Lesson via drag-and-drop reschedule or manual edit.
 *
 * <p>Key difference from {@link CreateLessonRequest}: {@code durationMinutes} is a
 * boxed {@code Integer} (not a primitive) so the deserializer can distinguish
 * "not sent" from zero, and both {@code timeslotId} / {@code roomId} are
 * explicitly nullable — the endpoint must NOT throw 400 when they are null.
 *
 * <p>Pinned-lesson protection: if the persisted lesson already has
 * {@code isPinned = true}, the service layer will reject any attempt to change
 * {@code timeslotId} or {@code roomId} with a 409 Conflict.
 */
public record UpdateLessonRequest(
    @NotNull(message = "Teacher ID is required")
    Long teacherId,

    Long danceGroupId,   // null for private lessons
    Long studentId,      // null for group lessons; null means "solver template" for private

    @NotNull(message = "Duration is required")
    @Min(value = 15, message = "Duration must be at least 15 minutes")
    Integer durationMinutes,

    boolean isPrivate,
    boolean isPinned,
    boolean isActive,

    Long timeslotId,  // null → unassign timeslot
    Long roomId       // null → unassign room
) {}

