package com.timetable.backend.config;

import jakarta.annotation.PostConstruct;
import com.timetable.backend.domain.dto.SolverSettingsValues;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.function.Supplier;

/**
 * Externalized configuration for Timefold solver constraint weights and thresholds.
 *
 * <p>All values can be overridden in {@code application.properties} under the
 * {@code solver.weights.*} prefix without recompiling the application.</p>
 *
 * <p>Example override in application.properties:
 * <pre>
 *   solver.weights.reward-student-assignment=90
 *   solver.weights.penalty-same-day-student=80
 * </pre>
 * </p>
 */
@Configuration
@ConfigurationProperties(prefix = "solver.weights")
@Getter
@Setter
public class SolverWeightsConfig {

    /**
     * Static holder: populated by {@link #publishInstance()} after Spring initializes
     * this bean. Used by {@code DanceScheduleConstraintProvider}'s no-arg constructor,
     * which Timefold calls via reflection (Timefold 1.6.0 does not support Spring DI
     * for ConstraintProvider — it always instantiates via {@code newInstance()}).
     *
     * <p>Falls back to a fresh default instance so tests never get a null pointer.</p>
     */
    private static volatile SolverWeightsConfig INSTANCE;
    private static final ThreadLocal<SolverWeightsConfig> FACTORY_WEIGHTS = new ThreadLocal<>();

    /**
     * Returns the Spring-managed instance, or a default instance if Spring has not
     * yet initialized (e.g., in unit tests that build the constraint verifier directly).
     */
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

    private int physicalConflictHardWeight = 1;
    private int weeklyAvailabilityHardWeight = 1;
    private int teacherDailyLimitHardWeight = 1;
    private int studentMultipleDailyPrivateHardWeight = 1;
    private int weeklyQuotaOverflowHardWeight = 1;
    private int teacherGapPenaltyMultiplier = 1;
    private int teacherWorkloadFairnessMultiplier = 1;

    /** Called by Spring after all properties are bound. Publishes {@code this} to the static holder. */
    @PostConstruct
    void publishInstance() {
        INSTANCE = this;
    }

    /**
     * Soft reward for assigning a student to a private lesson.
     * Must dominate all penalties to keep matchmaking the primary objective.
     * Default: 100
     */
    private int rewardStudentAssignment = 100;

    /**
     * Soft reward per lesson scheduled during prime time (16:00–21:00).
     * Should be a fraction of rewardStudentAssignment so it does not override matchmaking.
     * Default: 10
     */
    private int rewardPrimeTime = 2;


    /**
     * Gaps up to this many minutes between two lessons of the same teacher
     * on the same day are treated as a normal break and are NOT penalized.
     * Default: 15
     */
    private long teacherGapThresholdMinutes = 15;

    /**
     * Hard limit on the number of lessons a single teacher may have in one day.
     * 6 × 60 min + 5 × 15 min break ≈ 6 h 15 min — a realistic daily maximum.
     * Default: 6
     */
    private int maxTeacherLessonsPerDay = 6;

    /**
     * Soft penalty per lesson missing from a student's desired weekly count.
     * Applied when a student has at least one lesson assigned but fewer than desired.
     * Should be less than rewardStudentAssignment so the solver always benefits from assigning.
     * Default: 50 (50% of assignment reward — meaningful but not dominant)
     */
    private int penaltyStudentUnderDesiredLessons = 50;

    /**
     * Soft penalty per lesson missing from a teacher's desired weekly count.
     * Applied when a teacher has fewer assigned lessons than their desiredLessonsPerWeek.
     * Default: 30
     */
    private int penaltyTeacherUnderDesiredLessons = 30;
}
