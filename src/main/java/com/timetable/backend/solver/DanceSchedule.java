package com.timetable.backend.solver;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import com.timetable.backend.domain.model.*;
import lombok.*;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

/**
 * Planning Solution for the dance school timetable problem.
 * Contains all problem facts (timeslots, rooms, teachers) and planning entities (lessons).
 * Timefold Solver will optimize the assignment of timeslots and rooms to lessons.
 */
@PlanningSolution
@Getter
@Setter
@NoArgsConstructor(force = true) // Required by Timefold for solution cloning/proxying.
@RequiredArgsConstructor
@ToString
@AllArgsConstructor
public class DanceSchedule {

    @PlanningId
    private final Long id;

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "timeslotRange")
    private final List<Timeslot> timeslotList;

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "roomRange")
    private final List<Room> roomList;

    @ProblemFactCollectionProperty
    private final List<Teacher> teacherList;

    @ProblemFactCollectionProperty
    private final List<ResourceUnavailability> resourceUnavailabilityList;

    // NEW: Added weekly availability facts
    @ProblemFactCollectionProperty
    private final List<WeeklyAvailability> weeklyAvailabilityList;

    @ProblemFactCollectionProperty
    private final List<LocalDate> scheduleDateAnchorList;

    @PlanningEntityCollectionProperty
    private final List<Lesson> lessonList;

    @PlanningScore
    private HardSoftScore score;


}
