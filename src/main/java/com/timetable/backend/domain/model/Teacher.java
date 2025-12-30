package com.timetable.backend.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.JoinTable;
import jakarta.persistence.JoinColumn;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "teachers")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class Teacher extends AbstractUser {

    @Min(0)
    @Column(name = "max_daily_hours")
    private int maxDailyHours = 8;

    // hex color like #RRGGBB or simple name
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

    public Teacher(Long id, String email, String passwordHash, String fullName, Role role, boolean isActive, int maxDailyHours, String colorCode) {
        super(id, email, passwordHash, fullName, role, isActive);
        this.maxDailyHours = maxDailyHours;
        this.colorCode = colorCode;
    }
}
