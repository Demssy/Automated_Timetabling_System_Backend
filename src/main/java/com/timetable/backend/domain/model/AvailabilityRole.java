package com.timetable.backend.domain.model;

/** Role under which a user's availability applies (relevant for dual-role users). */
public enum AvailabilityRole {
    TEACHER,
    STUDENT;

    /** Resolves the default availability role: STUDENT only for pure students, TEACHER otherwise. */
    public static AvailabilityRole defaultFor(AbstractUser user) {
        return user != null && user.hasRole("STUDENT") && !user.hasRole("TEACHER") ? STUDENT : TEACHER;
    }
}