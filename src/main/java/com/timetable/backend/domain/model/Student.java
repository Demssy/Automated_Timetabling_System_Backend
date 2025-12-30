package com.timetable.backend.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Past;
import lombok.*;

import java.time.LocalDate;

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

    public Student(Long id, String email, String passwordHash, String fullName, Role role, boolean isActive, LocalDate birthDate, DanceLevel danceLevel, String parentContact) {
        super(id, email, passwordHash, fullName, role, isActive);
        this.birthDate = birthDate;
        this.danceLevel = danceLevel;
        this.parentContact = parentContact;
    }
}
