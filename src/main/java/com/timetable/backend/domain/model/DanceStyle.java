package com.timetable.backend.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "dance_styles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"teachers"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DanceStyle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, unique = true)
    @NotBlank
    private String name;

    @ManyToMany(mappedBy = "danceStyles", fetch = FetchType.LAZY)
    private Set<Teacher> teachers = new HashSet<>();

    public DanceStyle(String name) {
        this.name = name;
    }
}

