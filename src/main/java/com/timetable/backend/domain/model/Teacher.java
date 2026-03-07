package com.timetable.backend.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "teachers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = false)
public class Teacher {

    @Id
    private Long id;

    /**
     * Reference to the base user record.
     * Uses shared primary key (@MapsId) — teacher.id == user.id.
     * Provides access to email, fullName, role, etc.
     */
    @OneToOne(fetch = FetchType.EAGER)
    @MapsId
    @JoinColumn(name = "id")
    private AbstractUser user;

    @Min(0)
    @Column(name = "max_daily_hours")
    private int maxDailyHours = 8;

    @Pattern(regexp = "^#?[A-Fa-f0-9]{6}$", message = "colorCode must be a 6-digit hex, optionally starting with #")
    @Column(name = "color_code")
    private String colorCode = "#000000";

    @ManyToMany
    @JoinTable(
        name = "teacher_dance_style",
        joinColumns = @JoinColumn(name = "teacher_id"),
        inverseJoinColumns = @JoinColumn(name = "dance_style_id")
    )
    private Set<DanceStyle> danceStyles = new HashSet<>();

}
