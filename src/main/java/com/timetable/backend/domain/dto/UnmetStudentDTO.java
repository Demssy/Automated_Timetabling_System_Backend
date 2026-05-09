package com.timetable.backend.domain.dto;

/**
 * Represents a student who did not receive all the lessons they wanted.
 *
 * <p>"Desired" is defined as the number of {@code WeeklyAvailability} windows
 * the student declared. Each window represents a time block when the student
 * is willing to come for a lesson. If the solver assigned fewer lessons than
 * the number of windows, the student has unmet demand.</p>
 *
 * @param studentId       the student's database ID
 * @param studentName     the student's full name
 * @param studentEmail    the student's email address
 * @param desiredSlots    number of weekly availability windows the student declared
 * @param assignedLessons number of private lessons actually assigned in this schedule
 * @param missingLessons  desiredSlots - assignedLessons (always &gt; 0 in this response)
 */
public record UnmetStudentDTO(
    Long studentId,
    String studentName,
    String studentEmail,
    int desiredSlots,
    int assignedLessons,
    int missingLessons
) {}

