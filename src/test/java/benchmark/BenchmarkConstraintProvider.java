package benchmark;

import com.timetable.backend.config.SolverWeightsConfig;
import com.timetable.backend.solver.DanceScheduleConstraintProvider;

/**
 * Keeps benchmark weights independent of Spring configuration and application package scanning,
 * using the calibrated production weights.
 */
public final class BenchmarkConstraintProvider extends DanceScheduleConstraintProvider {

    public BenchmarkConstraintProvider() {
        super(calibratedDefaults());
    }

    public static SolverWeightsConfig calibratedDefaults() {
        SolverWeightsConfig config = new SolverWeightsConfig();
        config.setRewardStudentAssignment(60);
        config.setRewardPrimeTime(15);
        config.setPenaltyStudentUnderDesiredLessons(35);
        config.setPenaltyTeacherUnderDesiredLessons(25);
        config.setTeacherGapPenaltyMultiplier(8);
        config.setTeacherWorkloadFairnessMultiplier(3);
        config.setTeacherGapThresholdMinutes(15);
        config.setMaxTeacherLessonsPerDay(6);
        return config;
    }
}