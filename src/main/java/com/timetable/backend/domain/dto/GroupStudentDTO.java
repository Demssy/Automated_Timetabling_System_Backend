package com.timetable.backend.domain.dto;

import com.timetable.backend.domain.model.DanceLevel;

/**
 * Lightweight student projection returned by {@code GET /api/groups/{id}/students}.
 */
public record GroupStudentDTO(
        Long id,
        String fullName,
        String email,
        DanceLevel danceLevel
) {}
