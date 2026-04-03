package com.timetable.backend.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Past;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "students")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class Student extends AbstractUser {

    @Past
    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "dance_level")
    private DanceLevel danceLevel;

    @Column(name = "parent_contact")
    private String parentContact;


    @ManyToMany(mappedBy = "privateStudents", fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<Teacher> preferredTeachers = new HashSet<>();

}


