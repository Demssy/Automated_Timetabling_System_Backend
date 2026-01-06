package com.timetable.backend.domain.dto;

import com.timetable.backend.domain.model.DanceLevel;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

/**
 * DTO for updating student information.
 * All fields are optional - only provided fields will be updated.
 */
public record UpdateStudentRequest(
    String fullName,

    @Past(message = "Birth date must be in the past")
    LocalDate birthDate,

    DanceLevel danceLevel,

    String parentContact
) {}

