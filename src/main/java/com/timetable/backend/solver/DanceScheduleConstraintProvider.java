package com.timetable.backend.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import com.timetable.backend.domain.model.Lesson;

/**
 * Constraint provider for dance schedule optimization.
 * Defines hard and soft constraints for the Timefold Solver.
 *
 * TODO: Implement constraints in BE-11:
 * - Hard: Room conflict (with Dual-Mode logic)
 * - Hard: Teacher conflict
 * - Hard: Teacher availability
 * - Soft: Minimize gaps
 */
public class DanceScheduleConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
            // Placeholder constraint to prevent Timefold autoconfiguration errors
            // Real constraints will be implemented in BE-11
            dummyConstraint(constraintFactory)
        };
    }

    /**
     * Temporary placeholder constraint.
     * Will be replaced with real constraints in BE-11.
     */
    private Constraint dummyConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() == null || lesson.getRoom() == null)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Lesson must have timeslot and room assigned");
    }
}

