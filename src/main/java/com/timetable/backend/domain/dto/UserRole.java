package com.timetable.backend.domain.dto;

/**
 * Enum representing the allowed roles for public self-registration.
 * ADMIN is present for completeness but must be rejected at the service layer.
 */
public enum UserRole {
    STUDENT,
    TEACHER,
    ADMIN
}

