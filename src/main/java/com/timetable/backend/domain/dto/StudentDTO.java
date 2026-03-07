package com.timetable.backend.domain.dto;

import com.timetable.backend.domain.model.DanceLevel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record StudentDTO(
    Long id,
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    String email,
    @NotBlank(message = "Full name is required")
    String fullName,
    String password, // Optional for updates
    @Past(message = "Birth date must be in past")
    @NotNull(message = "Birth date is required")
    LocalDate birthDate,
    DanceLevel danceLevel,
    String parentContact
) {}

