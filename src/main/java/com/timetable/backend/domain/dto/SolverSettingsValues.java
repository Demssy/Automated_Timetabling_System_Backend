package com.timetable.backend.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SolverSettingsValues(
        @NotNull @Min(5) @Max(300) Integer solverTimeLimitSeconds,
        @NotNull @Min(0) Integer maxAllowedHardPenalty,
        @NotNull @Min(1) Integer teacherMaxDailyLessons,
        @NotNull @Min(0) Integer teacherBreakThresholdMinutes,
        @NotNull @Min(1) @Max(50) Integer physicalConflictHardWeight,
        @NotNull @Min(1) @Max(20) Integer weeklyAvailabilityHardWeight,
        @NotNull @Min(0) @Max(20) Integer teacherDailyLimitHardWeight,
        @NotNull @Min(0) @Max(20) Integer studentMultipleDailyPrivateHardWeight,
        @NotNull @Min(1) @Max(20) Integer weeklyQuotaOverflowHardWeight,
        @NotNull @Min(0) @Max(100) Integer privateLessonAssignmentReward,
        @NotNull @Min(0) @Max(100) Integer primeTimeLessonReward,
        @NotNull @Min(0) @Max(100) Integer studentUnfulfilledLessonPenalty,
        @NotNull @Min(0) @Max(100) Integer teacherUnfulfilledLessonPenalty,
        @NotNull @Min(0) @Max(100) Integer teacherGapPenaltyMultiplier,
        @NotNull @Min(0) @Max(100) Integer teacherWorkloadFairnessMultiplier
) {
}
