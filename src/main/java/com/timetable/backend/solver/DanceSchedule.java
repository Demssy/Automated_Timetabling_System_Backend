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
 * Contains all problem facts (timeslots, teachers) and planning entities (lessons).
 * Timefold Solver assigns timeslots to private lessons and students from the pool.
 * Room is NOT a planning variable (single room, pre-assigned).
 * Group lessons are always pinned — the solver respects their time/teacher.
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
    private final List<Teacher> teacherList;

    @ProblemFactCollectionProperty
    private final List<ResourceUnavailability> resourceUnavailabilityList;

    // Added weekly availability facts
    @ProblemFactCollectionProperty
    private final List<WeeklyAvailability> weeklyAvailabilityList;

    @ProblemFactCollectionProperty
    private final List<LocalDate> scheduleDateAnchorList;

    /**
     * Pool of students available for assignment to private lesson templates.
     * Exposed as a ValueRangeProvider so the solver can assign them to Lesson.student.
     */
    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "studentRange")
    private final List<Student> studentList;

    @PlanningEntityCollectionProperty
    private final List<Lesson> lessonList;

    @PlanningScore
    private HardSoftScore score;


}
