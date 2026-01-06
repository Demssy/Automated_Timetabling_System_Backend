package com.timetable.backend.solver.domain;

import lombok.*;

/**
 * Planning Fact - Room (Pure POJO - NO JPA).
 * Represents a physical room that can be assigned to lessons.
 * Lightweight immutable reference for solver.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PlanningRoom {

    @EqualsAndHashCode.Include
    private Long id;

    private String name;
    private int capacity;
    private boolean allowsParallelPrivate;
}

