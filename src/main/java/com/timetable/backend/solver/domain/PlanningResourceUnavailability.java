package com.timetable.backend.solver.domain;

import lombok.*;

/**
 * Planning Fact - Teacher Unavailability (Pure POJO - NO JPA).
 * Represents a constraint: teacher cannot work during this timeslot.
 * Lightweight immutable reference for solver.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PlanningResourceUnavailability {

    @EqualsAndHashCode.Include
    private Long id;

    private Long teacherId;      // Reference to PlanningTeacher.id
    private Long timeslotId;     // Reference to PlanningTimeslot.id
    private String reason;
}

