package com.timetable.backend.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a group of students with common characteristics.
 * Used as a Problem Fact in Timefold Solver.
 */
@Entity
@Table(name = "dance_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DanceGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    @NotBlank
    private String name;

    @ManyToOne
    @JoinColumn(name = "dance_style_id")
    private DanceStyle danceStyle;

    @Enumerated(EnumType.STRING)
    @Column(name = "dance_level")
    private DanceLevel danceLevel;

    @Min(1)
    @Column(name = "min_size")
    private Integer minSize;

    @Column(name = "target_age_range")
    private String targetAgeRange;

    /**
     * Students currently enrolled in this group.
     * NOTE: This collection is NOT used by the Timefold Solver — it is only accessed
     * by the enrollment API. The solver only reads DanceGroup identity via Lesson.danceGroup.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "student_groups",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    @ToString.Exclude
    private Set<Student> enrolledStudents = new HashSet<>();

    @Version
    @Column(name = "version")
    private Integer version;

    public DanceGroup(String name, DanceStyle danceStyle, DanceLevel danceLevel) {
        this.name = name;
        this.danceStyle = danceStyle;
        this.danceLevel = danceLevel;
    }
}

