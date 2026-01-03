package com.timetable.backend.solver;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import com.timetable.backend.domain.model.Lesson;
import com.timetable.backend.domain.model.ResourceUnavailability;
import com.timetable.backend.domain.model.Room;
import com.timetable.backend.domain.model.Teacher;
import com.timetable.backend.domain.model.Timeslot;
import lombok.*;

import java.util.List;

/**
 * Planning Solution for the dance school timetable problem.
 * Contains all problem facts (timeslots, rooms, teachers) and planning entities (lessons).
 * Timefold Solver will optimize the assignment of timeslots and rooms to lessons.
 */
@PlanningSolution
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DanceSchedule {

    @PlanningId
    private Long id;

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "timeslotRange")
    private List<Timeslot> timeslotList;

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "roomRange")
    private List<Room> roomList;

    @ProblemFactCollectionProperty
    private List<Teacher> teacherList;

    @ProblemFactCollectionProperty
    private List<ResourceUnavailability> resourceUnavailabilityList;

    @PlanningEntityCollectionProperty
    private List<Lesson> lessonList;

    @PlanningScore
    private HardSoftScore score;

    public DanceSchedule(Long id, List<Timeslot> timeslotList, List<Room> roomList,
                         List<Teacher> teacherList, List<ResourceUnavailability> resourceUnavailabilityList,
                         List<Lesson> lessonList) {
        this.id = id;
        this.timeslotList = timeslotList;
        this.roomList = roomList;
        this.teacherList = teacherList;
        this.resourceUnavailabilityList = resourceUnavailabilityList;
        this.lessonList = lessonList;
    }
}
