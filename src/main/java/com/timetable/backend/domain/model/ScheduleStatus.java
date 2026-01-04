package com.timetable.backend.domain.model;

/**
 * Enum representing the lifecycle status of a schedule.
 */
public enum ScheduleStatus {
    /**
     * Draft schedule - work in progress, editable
     */
    DRAFT,

    /**
     * Published schedule - active and in use
     */
    PUBLISHED,

    /**
     * Archived schedule - historical record, read-only
     */
    ARCHIVED
}

