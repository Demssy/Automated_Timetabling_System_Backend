package com.timetable.backend.solver.domain;

import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Planning Fact - Timeslot (Pure POJO - NO JPA).
 * Represents a time slot that can be assigned to lessons.
 * Lightweight immutable reference for solver.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PlanningTimeslot {

    @EqualsAndHashCode.Include
    private Long id;

    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
}

