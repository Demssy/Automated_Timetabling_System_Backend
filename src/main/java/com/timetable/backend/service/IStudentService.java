package com.timetable.backend.service;

import com.timetable.backend.domain.dto.StudentResponse;
import com.timetable.backend.domain.model.DanceLevel;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for student management operations.
 * Handles student CRUD operations and queries.
 */
public interface IStudentService {

    /**
     * Retrieves all students in the system.
     *
     * @return list of all students
     */
    List<StudentResponse> getAllStudents();

    /**
     * Retrieves a student by ID.
     *
     * @param id the student identifier
     * @return student response DTO
     * @throws com.timetable.backend.domain.exception.ResourceNotFoundException if student not found
     */
    StudentResponse getStudentById(Long id);

    /**
     * Updates student information.
     *
     * @param id the student identifier
     * @param fullName updated full name (optional)
     * @param birthDate updated birth date (optional)
     * @param danceLevel updated dance level (optional)
     * @param parentContact updated parent contact (optional)
     * @return updated student response DTO
     * @throws com.timetable.backend.domain.exception.ResourceNotFoundException if student not found
     */
    StudentResponse updateStudent(Long id, String fullName, LocalDate birthDate,
                                   DanceLevel danceLevel, String parentContact);

    /**
     * Deletes a student by ID.
     *
     * @param id the student identifier
     * @throws com.timetable.backend.domain.exception.ResourceNotFoundException if student not found
     */
    void deleteStudent(Long id);

    /**
     * Finds students by dance level.
     *
     * @param danceLevel the dance level to filter by
     * @return list of students with specified dance level
     */
    List<StudentResponse> getStudentsByDanceLevel(DanceLevel danceLevel);
}

