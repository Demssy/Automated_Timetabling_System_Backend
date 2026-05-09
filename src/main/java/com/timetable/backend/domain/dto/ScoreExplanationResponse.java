package com.timetable.backend.domain.dto;

import java.util.List;

/**
 * Full score explanation for a solved schedule.
 *
 * @param scheduleId the schedule identifier
 * @param totalScore total score string (e.g. "-2hard/+15soft")
 * @param violations all constraints that contributed a non-zero score,
 *                   sorted with hard violations first
 */
public record ScoreExplanationResponse(
    Long scheduleId,
    String totalScore,
    List<ConstraintViolationSummary> violations
) {}

