package com.timetable.backend.solver.domain;

import lombok.*;

/**
 * Planning Fact - Teacher (Pure POJO - NO JPA).
 * Represents a teacher resource in the solver.
 * Lightweight immutable reference for solver.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PlanningTeacher {

    @EqualsAndHashCode.Include
    private Long id;

    private String fullName;
    private String email;
    private int maxDailyHours;
    private String colorCode;
}

