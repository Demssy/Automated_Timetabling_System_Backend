package com.timetable.backend.domain.dto;

/**
 * Request body for cancelling a scheduled lesson.
 *
 * @param reason Optional human-readable reason for cancellation (may be null).
 */
public record CancelLessonRequest(String reason) {
}

