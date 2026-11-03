package benchmark;

import com.timetable.backend.config.SolverWeightsConfig;
import com.timetable.backend.solver.DanceScheduleConstraintProvider;

/** Keeps benchmark weights independent of Spring configuration and application package scanning. */
public final class BenchmarkConstraintProvider extends DanceScheduleConstraintProvider {
    public BenchmarkConstraintProvider() {
        super(new SolverWeightsConfig());
    }
}
