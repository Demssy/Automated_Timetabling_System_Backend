package com.timetable.backend.domain.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a time period when a teacher is unavailable.
 * Used in Hard Constraint: lessons cannot be scheduled when teacher is unavailable.
 */
@Entity
@Table(name = "resource_unavailability")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ResourceUnavailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne
    @JoinColumn(name = "timeslot_id", nullable = false)
    private Timeslot timeslot;

    @Column(name = "reason")
    private String reason;

    public ResourceUnavailability(Teacher teacher, Timeslot timeslot, String reason) {
        this.teacher = teacher;
        this.timeslot = timeslot;
        this.reason = reason;
    }
}

