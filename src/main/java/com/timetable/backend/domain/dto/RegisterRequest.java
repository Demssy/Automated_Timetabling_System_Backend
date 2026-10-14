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

    /** Role chosen during registration. ADMIN is rejected by the service layer. */
    @NotNull(message = "Role is required")
    UserRole role,

    // ── Student-only ──────────────────────────────────────────────────────────
    DanceLevel danceLevel,

    /** Emergency/parent contact; recommended when the student is under 18. */
    String parentContact,

    /** How many private lessons per week the student would like. */
    Integer desiredLessonsPerWeek,

    // ── Teacher-only ──────────────────────────────────────────────────────────
    /** Teacher's contact phone number. */
    String phone,

    /**
     * Dance styles the teacher is qualified to teach.
     * Accepts either:
     * - IDs (Long): [1, 2, 3]
     * - Names (String): ["SALSA", "BACHATA"]
     * Must contain at least one element when role == TEACHER.
     * Type: List of Long/Integer/String (mixed allowed).
     */
    List<?> qualifiedStyleIds,

    /** Short teacher biography shown on the public profile. */
    String bio,

    // -- Dual role (TEACHER who also attends lessons as a STUDENT) --

    /** When true and role == TEACHER, the account also gets the STUDENT role and a student profile. */
    boolean alsoStudent,

    /** Dance level of the student profile; falls back to {@code danceLevel} when null. */
    DanceLevel studentLevel,

    /** Dance styles the user wants to learn as a student (optional). */
    Set<Long> studentPreferredStyleIds
) {
    /** Convenience constructor for single-role registration. */
    public RegisterRequest(
            String email, String password, String fullName, LocalDate birthDate, UserRole role,
            DanceLevel danceLevel, String parentContact, Integer desiredLessonsPerWeek,
            String phone, List<?> qualifiedStyleIds, String bio) {
        this(email, password, fullName, birthDate, role, danceLevel, parentContact, desiredLessonsPerWeek,
                phone, qualifiedStyleIds, bio, false, null, null);
    }
}
