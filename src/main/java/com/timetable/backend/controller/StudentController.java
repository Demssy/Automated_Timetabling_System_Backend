package com.timetable.backend.controller;

import com.timetable.backend.domain.dto.StudentResponse;
import com.timetable.backend.domain.dto.UpdateStudentRequest;
import com.timetable.backend.domain.model.DanceLevel;
import com.timetable.backend.service.IStudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for student management operations.
 * Provides endpoints for CRUD operations on students.
 */
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final IStudentService studentService;

    /**
     * Retrieves all students in the system.
     * Accessible by ADMIN and TEACHER roles.
     *
     * @return list of all students
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<StudentResponse>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents());
    }

    /**
     * Retrieves a specific student by ID.
     * Accessible by ADMIN, TEACHER, and the student themselves.
     *
     * @param id the student identifier
     * @return student response DTO
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER') or (hasRole('STUDENT') and #id == authentication.principal.id)")
    public ResponseEntity<StudentResponse> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    /**
     * Updates student information.
     * Accessible by ADMIN and the student themselves.
     *
     * @param id the student identifier
     * @param request the update request DTO
     * @return updated student response DTO
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('STUDENT') and #id == authentication.principal.id)")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStudentRequest request) {

        var updated = studentService.updateStudent(
            id,
            request.fullName(),
            request.birthDate(),
            request.danceLevel(),
            request.parentContact()
        );

        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a student by ID.
     * Accessible only by ADMIN role.
     *
     * @param id the student identifier
     * @return no content response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves students filtered by dance level.
     * Accessible by ADMIN and TEACHER roles.
     *
     * @param danceLevel the dance level to filter by
     * @return list of students with specified dance level
     */
    @GetMapping("/by-level/{danceLevel}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<StudentResponse>> getStudentsByDanceLevel(
            @PathVariable DanceLevel danceLevel) {
        return ResponseEntity.ok(studentService.getStudentsByDanceLevel(danceLevel));
    }
}

