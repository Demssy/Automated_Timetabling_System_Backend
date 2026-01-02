package com.timetable.backend.domain.dto;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;

import java.util.List;

/**
 * Response containing the optimized schedule solution.
 *
 * @param scheduleId the schedule identifier
 * @param score the final score (hardScore, softScore)
 * @param hardScore number of hard constraint violations (should be 0 for valid solution)
 * @param softScore quality score (higher is better)
 * @param fullyAssigned whether all lessons have timeslots and rooms assigned
 * @param lessons list of scheduled lessons
 */
public record ScheduleSolutionResponse(
    Long scheduleId,
    String score,
    int hardScore,
    int softScore,
    boolean fullyAssigned,
    List<ScheduledLessonDTO> lessons
) {
    public static ScheduleSolutionResponse from(
        Long scheduleId,
        HardSoftScore score,
        boolean fullyAssigned,
        List<ScheduledLessonDTO> lessons
    ) {
        return new ScheduleSolutionResponse(
            scheduleId,
            score != null ? score.toString() : "N/A",
            score != null ? score.hardScore() : 0,
            score != null ? score.softScore() : 0,
            fullyAssigned,
            lessons
        );
    }
}

