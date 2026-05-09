package com.timetable.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Externalized configuration for Timefold solver constraint weights and thresholds.
 *
 * <p>All values can be overridden in {@code application.properties} under the
 * {@code solver.weights.*} prefix without recompiling the application.</p>
 *
 * <p>Example override in application.properties:
 * <pre>
 *   solver.weights.reward-student-assignment=120
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

    /**
     * Returns the Spring-managed instance, or a default instance if Spring has not
     * yet initialized (e.g., in unit tests that build the constraint verifier directly).
     */
    public static SolverWeightsConfig getInstance() {
        return INSTANCE != null ? INSTANCE : new SolverWeightsConfig();
    }

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
    private int rewardPrimeTime = 10;


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
}

