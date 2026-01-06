package com.timetable.backend.solver.domain;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import lombok.*;

import java.util.List;

/**
 * Planning Solution for Timefold Solver (Pure POJO - NO JPA).
 *
 * This is the main class that Timefold Solver works with during optimization.
 * Contains all problem facts and planning entities in lightweight form.
 *
 * Separation from JPA DanceSchedule provides:
 * - Zero Hibernate overhead during solving
 * - No LazyInitializationException
 * - Fast cloning (no proxy unwrapping)
 * - Clean separation of concerns
 */
@PlanningSolution
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TimetableSolution {

    @PlanningId
    private Long id;

    // Problem Facts (immutable during solving)

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "timeslotRange")
    private List<PlanningTimeslot> timeslotList;

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "roomRange")
    private List<PlanningRoom> roomList;

    @ProblemFactCollectionProperty
    private List<PlanningTeacher> teacherList;

    @ProblemFactCollectionProperty
    private List<PlanningResourceUnavailability> resourceUnavailabilityList;

    // Planning Entities (Timefold will assign timeslot/room to these)

    @PlanningEntityCollectionProperty
    private List<PlanningLesson> lessonList;

    // Solution Score

    @PlanningScore
    private HardSoftScore score;

    /**
     * Constructor without score (for initial problem setup).
     */
    public TimetableSolution(Long id,
                            List<PlanningTimeslot> timeslotList,
                            List<PlanningRoom> roomList,
                            List<PlanningTeacher> teacherList,
                            List<PlanningResourceUnavailability> resourceUnavailabilityList,
                            List<PlanningLesson> lessonList) {
        this.id = id;
        this.timeslotList = timeslotList;
        this.roomList = roomList;
        this.teacherList = teacherList;
        this.resourceUnavailabilityList = resourceUnavailabilityList;
        this.lessonList = lessonList;
    }
}

