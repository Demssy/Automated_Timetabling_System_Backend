package com.timetable.backend.domain.dto;

/**
 * Represents a student who did not receive all the lessons they wanted.
 *
 * <p>"Desired" is defined by the student's explicit {@code desiredLessonsPerWeek}.
 * If the solver assigned fewer non-cancelled private lessons than requested,
 * the student has unmet demand.</p>
 *
 * @param studentId       the student's database ID
 * @param studentName     the student's full name
 * @param studentEmail    the student's email address
 * @param desiredSlots    number of weekly private lessons requested by the student
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

