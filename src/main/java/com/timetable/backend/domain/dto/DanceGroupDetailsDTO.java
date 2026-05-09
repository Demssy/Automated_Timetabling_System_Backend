package com.timetable.backend.domain.dto;

import com.timetable.backend.domain.model.DanceLevel;

import java.util.List;

/**
 * Detailed view of a dance group, including its weekly schedule and enrollment status.
 * Used by the public "Groups" page and the "My Groups" tab.
 */
public record DanceGroupDetailsDTO(
    Long id,
    String name,
    String danceStyleName,
    DanceLevel danceLevel,
    String targetAgeRange,
    Integer minSize,
    /** Ordered list of weekly timeslots derived from the group's pinned lessons. */
    List<GroupScheduleSlotDTO> schedule,
    /** Total number of enrolled students. */
    int enrolledCount,
    /** True if the currently authenticated student is enrolled in this group. */
    boolean enrolledByCurrentUser
) {}

