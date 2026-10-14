package com.timetable.backend.domain.dto;

import java.util.List;
import java.util.Set;

/**
 * DTO for authenticated user information.
 * Contains common user fields and role information.
 * Specific user type details (Student, Teacher, Admin) should be obtained via dedicated endpoints.
 *
 * @param id user identifier
 * @param email user's email address
 * @param fullName user's full name
 * @param role user's primary role name (e.g., "STUDENT", "TEACHER", "ADMIN")
 * @param isActive whether the user account is active
 * @param roles all granted authorities, e.g. ["ROLE_TEACHER", "ROLE_STUDENT"]
 */
public record UserResponse(
    Long id,
    String email,
    String fullName,
    String role,
    boolean isActive,
    List<WeeklyAvailabilityDTO> weeklyAvailabilities,
    List<ResourceUnavailabilityDTO> oneTimeUnavailabilities,
    Set<String> roles
) {
    /** Convenience constructor for single-role users: roles is derived from the primary role. */
    public UserResponse(
            Long id,
            String email,
            String fullName,
            String role,
            boolean isActive,
            List<WeeklyAvailabilityDTO> weeklyAvailabilities,
            List<ResourceUnavailabilityDTO> oneTimeUnavailabilities
    ) {
        this(id, email, fullName, role, isActive, weeklyAvailabilities, oneTimeUnavailabilities,
                role == null ? Set.of() : Set.of("ROLE_" + role));
    }
}

