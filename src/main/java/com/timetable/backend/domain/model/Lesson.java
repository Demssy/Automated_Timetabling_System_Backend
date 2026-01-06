package com.timetable.backend.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

/**
 * JPA Entity representing a lesson in the database.
 *
 * REFACTORED: Timefold annotations removed and moved to PlanningLesson.
 * This entity is now used ONLY for persistence.
 * For solving, convert to PlanningLesson via PlanningModelMapper.
 */
@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"teacher", "danceGroup", "timeslot", "room"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dance_group_id", nullable = false)
    private DanceGroup danceGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timeslot_id")
    private Timeslot timeslot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Min(15)
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 60;

    @Column(name = "is_pinned", nullable = false)
    private boolean pinned = false;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate = false;

    public Lesson(Teacher teacher, DanceGroup danceGroup, int durationMinutes, boolean isPrivate) {
        this.teacher = teacher;
        this.danceGroup = danceGroup;
        this.durationMinutes = durationMinutes;
        this.isPrivate = isPrivate;
    }
}

