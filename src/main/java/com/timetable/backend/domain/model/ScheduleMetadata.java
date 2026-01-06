package com.timetable.backend.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing metadata for a schedule version.
 * Allows tracking multiple schedule versions, their validity periods, and status.
 */
@Entity
@Table(name = "schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ScheduleMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Version
    private Long version;

    /**
     * Human-readable name for the schedule (e.g., "Fall Semester 2025", "Summer Camp Draft")
     */
    @Column(nullable = false)
    private String name;

    /**
     * First date this schedule is valid/active
     */
    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    /**
     * Last date this schedule is valid/active
     */
    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    /**
     * Timestamp when this schedule was created
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Current lifecycle status of the schedule
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleStatus status;

    /**
     * Solver score representation (e.g., "0hard/-200soft")
     * Indicates the quality of the solution
     */
    @Column(name = "solver_score", length = 50)
    private String solverScore;

    /**
     * Optional description or notes about this schedule
     */
    @Column(length = 500)
    private String description;

    /**
     * Sets createdAt timestamp before persisting
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

