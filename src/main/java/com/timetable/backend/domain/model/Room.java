package com.timetable.backend.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank
    private String name;

    @Min(1)
    private int capacity;

    @Column(name = "allows_parallel_private")
    private boolean allowsParallelPrivate = false;

    public Room(String name, int capacity, boolean allowsParallelPrivate) {
        this.name = name;
        this.capacity = capacity;
        this.allowsParallelPrivate = allowsParallelPrivate;
    }
}

