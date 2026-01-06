package com.timetable.backend.solver.domain;

import com.timetable.backend.domain.model.DanceLevel;
import lombok.*;

/**
 * Planning Fact - Dance Group (Pure POJO - NO JPA).
 * Represents a student group in the solver.
 * Lightweight immutable reference for solver.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PlanningDanceGroup {

    @EqualsAndHashCode.Include
    private Long id;

    private String name;
    private Long danceStyleId; // Just ID reference to avoid deep nesting
    private DanceLevel danceLevel;
    private Integer minSize;
}

