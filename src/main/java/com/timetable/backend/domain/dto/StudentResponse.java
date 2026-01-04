package com.timetable.backend.domain.dto;

import com.timetable.backend.domain.model.DanceLevel;

import java.time.LocalDate;

/**
 * DTO for safe representation of Student data.
 * Excludes sensitive information like password hash and system fields.
 *
 * @param id student identifier
 * @param email student's email address
 * @param fullName student's full name
 * @param birthDate student's date of birth
 * @param danceLevel current dance skill level
 * @param parentContact parent/guardian contact information
 */
public record StudentResponse(
    Long id,
    String email,
    String fullName,
    LocalDate birthDate,
    DanceLevel danceLevel,
    String parentContact
) {}

