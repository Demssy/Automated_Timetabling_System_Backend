package com.timetable.backend.domain.dto;

import ai.timefold.solver.core.api.solver.SolverStatus;

/**
 * Response for solver status requests.
 *
 * @param scheduleId the schedule identifier
 * @param status current solver status (NOT_SOLVING, SOLVING_SCHEDULED, SOLVING_ACTIVE)
 * @param message human-readable status message
 */
public record SolverStatusResponse(
    Long scheduleId,
    SolverStatus status,
    String message
) {
    public static SolverStatusResponse of(Long scheduleId, SolverStatus status) {
        String message = switch (status) {
            case NOT_SOLVING -> "Solver is not currently running for this schedule";
            case SOLVING_SCHEDULED -> "Solving is scheduled but not yet started";
            case SOLVING_ACTIVE -> "Solver is actively optimizing the schedule";
        };

        return new SolverStatusResponse(scheduleId, status, message);
    }
}

