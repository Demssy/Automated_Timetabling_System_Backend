package com.timetable.backend.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    /** Solver-assigned student for private lesson templates. Null for group lessons. */
    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    // ── Cancellation fields ───────────────────────────────────────────────────

    /**
     * Manual cancellation flag set by a teacher or admin.
     * Intentionally separate from {@link ScheduledLessonStatus} so that
     * the solver never overwrites a human cancellation decision.
     */
    @Column(name = "is_cancelled", nullable = false)
    private boolean cancelled = false;

    /**
     * The user (teacher or admin) who cancelled this lesson.
     * Null if the lesson has not been cancelled.
     */
    @ManyToOne
    @JoinColumn(name = "cancelled_by")
    private AbstractUser cancelledBy;

    /** Timestamp when the cancellation was recorded. */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /** Optional human-readable reason for the cancellation. */
    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;
}
