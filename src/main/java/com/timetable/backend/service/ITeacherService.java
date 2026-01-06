package com.timetable.backend.service;

import com.timetable.backend.domain.dto.CreateTeacherRequest;
import com.timetable.backend.domain.dto.TeacherResponse;

/**
 * Service interface for teacher management operations.
 * Handles teacher creation, updates, and retrieval.
 */
public interface ITeacherService {

    /**
     * Creates a new teacher in the system.
     *
     * @param request the DTO containing teacher details
     * @return the created teacher as a response DTO
     * @throws com.timetable.backend.domain.exception.BusinessRuleViolationException if email already exists or dance style IDs are invalid
     */
    TeacherResponse createTeacher(CreateTeacherRequest request);
}

