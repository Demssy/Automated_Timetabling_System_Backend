package com.timetable.backend.domain.dto;

import java.util.List;

/**
 * Detailed breakdown of a single constraint match instance.
 *
 * @param hardImpact    hard score impact of this match
 * @param softImpact    soft score impact of this match
 * @param description   human-readable explanation
 * @param involvedUsers names/emails of teachers or students involved
 * @param lessonInfo    lesson summary (nullable)
 * @param timeslotInfo  timeslot summary (nullable)
 */
public record ConstraintMatchDetailDTO(
    int hardImpact,
    int softImpact,
    String description,
    List<String> involvedUsers,
    String lessonInfo,
    String timeslotInfo
) {}
