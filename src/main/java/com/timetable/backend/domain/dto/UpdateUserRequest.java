package com.timetable.backend.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for updating user information by admin.
 *
 * @param email user's email address
 * @param fullName user's full name
 * @param role user's role name (e.g., "STUDENT", "TEACHER", "ADMIN")
 * @param isActive whether the user account is active
 */
public record UpdateUserRequest(
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    String email,

    @NotBlank(message = "Full name is required")
    String fullName,

    String role,

    Boolean isActive
) {}

