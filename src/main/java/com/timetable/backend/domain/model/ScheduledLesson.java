package com.timetable.backend.domain.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Stores a concrete lesson assignment for a specific schedule version.
 */
@Entity
@Table(name = "scheduled_lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ScheduledLesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne
    @JoinColumn(name = "schedule_id", nullable = false)
    private ScheduleMetadata schedule;

    @ManyToOne
    @JoinColumn(name = "timeslot_id")
    private Timeslot timeslot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduledLessonStatus status;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;
}
