package com.timetable.backend.domain.model;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

/**
 * Represents a lesson that needs to be scheduled.
 * This is the main planning entity - Timefold Solver will assign timeslot and room.
 */
@Entity
@Table(name = "lessons")
@PlanningEntity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @PlanningId
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne
    @JoinColumn(name = "dance_group_id", nullable = true)
    private DanceGroup danceGroup;

    /** Assigned only for private lessons ({@code isPrivate = true}). Null for group lessons. */
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = true)
    private Student student;

    @PlanningVariable(valueRangeProviderRefs = "timeslotRange")
    @ManyToOne
    @JoinColumn(name = "timeslot_id")
    private Timeslot timeslot;

    @PlanningVariable(valueRangeProviderRefs = "roomRange")
    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @Min(15)
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 60;

    @PlanningPin
    @Column(name = "is_pinned", nullable = false)
    private boolean pinned = false;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate = false;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Version
    @Column(name = "version")
    private Integer version;

    /** Convenience constructor for group lessons (student == null). */
    public Lesson(Teacher teacher, DanceGroup danceGroup, int durationMinutes, boolean isPrivate, boolean isActive) {
        this.teacher = teacher;
        this.danceGroup = danceGroup;
        this.durationMinutes = durationMinutes;
        this.isPrivate = isPrivate;
        this.isActive = isActive;
    }

    /** Convenience constructor for private lessons (danceGroup == null). */
    public Lesson(Teacher teacher, Student student, int durationMinutes, boolean isActive) {
        this.teacher = teacher;
        this.student = student;
        this.durationMinutes = durationMinutes;
        this.isPrivate = true;
        this.isActive = isActive;
    }
}

