package com.timetable.backend.domain.dto;

/**
 * Response when starting a solve operation.
 *
 * @param scheduleId the schedule identifier for tracking
 * @param message informational message
 * @param statusUrl URL to check solver status
 */
public record SolveResponse(
    Long scheduleId,
    String message,
    String statusUrl
) {
    public static SolveResponse started(Long scheduleId) {
        return new SolveResponse(
            scheduleId,
            "Schedule optimization started. Check status using the provided URL.",
            "/api/solver/status/" + scheduleId
        );
    }
}

