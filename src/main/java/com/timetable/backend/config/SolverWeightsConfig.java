package com.timetable.backend.config;

import com.timetable.backend.domain.dto.SolverSettingsValues;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

/**
 * Single source of truth for default Timefold solver constraint weights and thresholds.
 * Runtime overrides are managed dynamically via SolverSettingsService and persisted in the database.
 */
@Configuration
@ConfigurationProperties(prefix = "solver.weights")
@Getter
@Setter
public class SolverWeightsConfig {

    private static volatile SolverWeightsConfig INSTANCE;
    private static final ThreadLocal<SolverWeightsConfig> FACTORY_WEIGHTS = new ThreadLocal<>();

    public static SolverWeightsConfig getInstance() {
        SolverWeightsConfig factoryWeights = FACTORY_WEIGHTS.get();
        if (factoryWeights != null) {
            return factoryWeights;
        }
        return INSTANCE != null ? INSTANCE : new SolverWeightsConfig();
    }

    public static <T> T withFactoryWeights(SolverSettingsValues values, Supplier<T> action) {
        SolverWeightsConfig previous = FACTORY_WEIGHTS.get();
        SolverWeightsConfig config = new SolverWeightsConfig();
        config.setRewardStudentAssignment(values.privateLessonAssignmentReward());
        config.setRewardPrimeTime(values.primeTimeLessonReward());
        config.setTeacherGapThresholdMinutes(values.teacherBreakThresholdMinutes());
        config.setMaxTeacherLessonsPerDay(values.teacherMaxDailyLessons());
        config.setPenaltyStudentUnderDesiredLessons(values.studentUnfulfilledLessonPenalty());
        config.setPenaltyTeacherUnderDesiredLessons(values.teacherUnfulfilledLessonPenalty());
        config.setPhysicalConflictHardWeight(values.physicalConflictHardWeight());
        config.setWeeklyAvailabilityHardWeight(values.weeklyAvailabilityHardWeight());
        config.setTeacherDailyLimitHardWeight(values.teacherDailyLimitHardWeight());
        config.setStudentMultipleDailyPrivateHardWeight(values.studentMultipleDailyPrivateHardWeight());
        config.setWeeklyQuotaOverflowHardWeight(values.weeklyQuotaOverflowHardWeight());
        config.setTeacherGapPenaltyMultiplier(values.teacherGapPenaltyMultiplier());
        config.setTeacherWorkloadFairnessMultiplier(values.teacherWorkloadFairnessMultiplier());
        FACTORY_WEIGHTS.set(config);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                FACTORY_WEIGHTS.remove();
            } else {
                FACTORY_WEIGHTS.set(previous);
            }
        }
    }

    @PostConstruct
    void publishInstance() {
        INSTANCE = this;
    }

    // Hard constraint weights
    private int physicalConflictHardWeight = 1;
    private int weeklyAvailabilityHardWeight = 1;
    private int teacherDailyLimitHardWeight = 1;
    private int studentMultipleDailyPrivateHardWeight = 1;
    private int weeklyQuotaOverflowHardWeight = 1;

    // Operational thresholds
    private long teacherGapThresholdMinutes = 15;
    private int maxTeacherLessonsPerDay = 6;

    // Calibrated soft constraint rewards & penalties
    private int rewardStudentAssignment = 60;
    private int rewardPrimeTime = 15;
    private int penaltyStudentUnderDesiredLessons = 35;
    private int penaltyTeacherUnderDesiredLessons = 25;
    private int teacherGapPenaltyMultiplier = 8;
    private int teacherWorkloadFairnessMultiplier = 3;
}