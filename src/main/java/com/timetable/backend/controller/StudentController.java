package com.timetable.backend.controller;

import com.timetable.backend.domain.dto.StudentDTO;
import com.timetable.backend.domain.dto.TeacherResponse;
import com.timetable.backend.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    // ──────────────────────────────────────────────────────────
    // Admin CRUD
    // ──────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StudentDTO>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentDTO> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentDTO> createStudent(@RequestBody @Valid StudentDTO request) {
        return ResponseEntity.ok(studentService.createStudent(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentDTO> updateStudent(
            @PathVariable Long id,
            @RequestBody @Valid StudentDTO request) {
        return ResponseEntity.ok(studentService.updateStudent(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────────
    // Student self-service: preferred-teacher management
    // ──────────────────────────────────────────────────────────

    /**
     * Returns the list of teachers the authenticated student has chosen for private lessons.
     */
    @GetMapping("/me/preferred-teachers")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<TeacherResponse>> getMyPreferredTeachers(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(studentService.getMyPreferredTeachers(email));
    }

    /**
     * Adds a teacher to the authenticated student's preferred-teacher pool.
     */
    @PostMapping("/me/preferred-teachers/{teacherId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> addPreferredTeacher(
            @PathVariable Long teacherId,
            Authentication authentication) {
        String email = authentication.getName();
        studentService.addMyTeacherPreference(email, teacherId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Removes a teacher from the authenticated student's preferred-teacher pool.
     */
    @DeleteMapping("/me/preferred-teachers/{teacherId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> removePreferredTeacher(
            @PathVariable Long teacherId,
            Authentication authentication) {
        String email = authentication.getName();
        studentService.removeMyTeacherPreference(email, teacherId);
        return ResponseEntity.noContent().build();
    }
}



