package com.timetable.backend.domain.dto;

import java.util.List;

/**
 * DTO for authenticated user information.
 * Contains common user fields and role information.
 * Specific user type details (Student, Teacher, Admin) should be obtained via dedicated endpoints.
 *
 * @param id user identifier
 * @param email user's email address
 * @param fullName user's full name
 * @param role user's role name (e.g., "STUDENT", "TEACHER", "ADMIN")
 * @param isActive whether the user account is active
 */
public record UserResponse(
    Long id,
    String email,
    String fullName,
    String role,
    boolean isActive,
    List<WeeklyAvailabilityDTO> weeklyAvailabilities,
    List<ResourceUnavailabilityDTO> oneTimeUnavailabilities
) {}

