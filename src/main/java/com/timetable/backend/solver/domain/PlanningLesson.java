package com.timetable.backend.solver.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import lombok.*;

/**
 * Planning Entity for Timefold Solver (Pure POJO - NO JPA).
 * Represents a lesson that needs timeslot and room assignment.
 *
 * This class is separated from JPA Lesson entity to:
 * - Avoid Hibernate proxy issues during solving
 * - Eliminate LazyInitializationException risks
 * - Improve cloning performance (lightweight)
 * - Prevent N+1 queries in solver
 */
@PlanningEntity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PlanningLesson {

    @PlanningId
    @EqualsAndHashCode.Include
    private Long id;

    // Problem Facts (immutable references during solving)
    private PlanningTeacher teacher;
    private PlanningDanceGroup danceGroup;

    // Planning Variables (Timefold will assign these)
    @PlanningVariable(valueRangeProviderRefs = "timeslotRange")
    private PlanningTimeslot timeslot;

    @PlanningVariable(valueRangeProviderRefs = "roomRange")
    private PlanningRoom room;

    // Lesson metadata
    private int durationMinutes;

    @PlanningPin
    private boolean pinned;

    private boolean isPrivate;

    /**
     * Convenience constructor for testing/initialization.
     */
    public PlanningLesson(Long id, PlanningTeacher teacher, PlanningDanceGroup danceGroup,
                         int durationMinutes, boolean isPrivate) {
        this.id = id;
        this.teacher = teacher;
        this.danceGroup = danceGroup;
        this.durationMinutes = durationMinutes;
        this.isPrivate = isPrivate;
        this.pinned = false;
    }
}

