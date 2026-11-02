package com.timetable.backend.domain.dto;

import com.timetable.backend.domain.model.DanceLevel;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * DTO for public self-registration.
 * Supports two roles: STUDENT and TEACHER.
 * Cross-field validation (e.g. TEACHER requires at least one specialization) is
 * enforced in the service layer.
 */
public record RegisterRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    String password,

    @NotBlank(message = "Full name is required")
    String fullName,

    @NotNull(message = "Birth date is required")
    @Past(message = "Birth date must be in the past")
    LocalDate birthDate,

    @NotNull(message = "Role is required")
    UserRole role,

    // ── Student-only ──────────────────────────────────────────────────────────
    DanceLevel danceLevel,

    String parentContact,

    Integer desiredLessonsPerWeek,

    // ── Teacher-only ──────────────────────────────────────────────────────────
    String phone,

    List<?> qualifiedStyleIds,

    String bio,

    String colorCode,

    // -- Dual role (TEACHER who also attends lessons as a STUDENT) --

    boolean alsoStudent,

    DanceLevel studentLevel,

    Set<Long> studentPreferredStyleIds
) {
    /** Convenience constructor for single-role registration. */
    public RegisterRequest(
            String email, String password, String fullName, LocalDate birthDate, UserRole role,
            DanceLevel danceLevel, String parentContact, Integer desiredLessonsPerWeek,
            String phone, List<?> qualifiedStyleIds, String bio) {
        this(email, password, fullName, birthDate, role, danceLevel, parentContact, desiredLessonsPerWeek,
                phone, qualifiedStyleIds, bio, null, false, null, null);
    }
}
