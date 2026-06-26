package com.timetable.backend.domain.dto;

import com.timetable.backend.domain.model.DanceLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

/**
 * DTO for a student updating their own profile.
 * Excludes email (immutable) and password (changed via separate endpoint).
 *
 * @param fullName         student's display name
 * @param birthDate        student's date of birth
 * @param danceLevel       current dance skill level
 * @param parentContact    parent/guardian contact information
 * @param desiredLessonsPerWeek preferred number of lessons per week
 */
public record UpdateStudentRequest(
    @NotBlank(message = "Full name is required")
    String fullName,

    @Past(message = "Birth date must be in the past")
    LocalDate birthDate,

    DanceLevel danceLevel,

    String parentContact,

    @Min(value = 0, message = "Desired lessons per week must be non-negative")
    Integer desiredLessonsPerWeek
) {}
