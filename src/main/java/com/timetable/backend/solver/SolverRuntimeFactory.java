package com.timetable.backend.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import com.timetable.backend.config.SolverWeightsConfig;
import com.timetable.backend.domain.dto.SolverSettingsValues;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class SolverRuntimeFactory {
    private final SolverConfig solverConfig;

    public SolverFactory<DanceSchedule> create(SolverSettingsValues settings) {
        SolverConfig config = solverConfig.copyConfig()
                .withTerminationSpentLimit(Duration.ofSeconds(settings.solverTimeLimitSeconds()));
        return SolverWeightsConfig.withFactoryWeights(settings, () -> SolverFactory.create(config));
    }

    public SolutionManager<DanceSchedule, HardSoftScore> solutionManager(SolverSettingsValues settings) {
        return SolutionManager.create(create(settings));
    }
}
