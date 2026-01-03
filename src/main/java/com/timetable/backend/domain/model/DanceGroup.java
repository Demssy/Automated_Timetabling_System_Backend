package com.timetable.backend.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

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

    public DanceGroup(String name, DanceStyle danceStyle, DanceLevel danceLevel) {
        this.name = name;
        this.danceStyle = danceStyle;
        this.danceLevel = danceLevel;
    }
}

