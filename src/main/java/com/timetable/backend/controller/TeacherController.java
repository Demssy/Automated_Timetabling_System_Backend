package com.timetable.backend.controller;

import com.timetable.backend.domain.dto.CreateTeacherRequest;
import com.timetable.backend.domain.dto.StudentAvailabilityResponse;
import com.timetable.backend.domain.dto.StudentResponse;
import com.timetable.backend.domain.dto.TeacherResponse;
import com.timetable.backend.domain.dto.UpdateTeacherRequest;
import com.timetable.backend.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    // ──────────────────────────────────────────────────────────
    // Admin CRUD
    // ──────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT', 'TEACHER')")
    public ResponseEntity<List<TeacherResponse>> getAllTeachers() {
        return ResponseEntity.ok(teacherService.getAllTeachers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeacherResponse> getTeacherById(@PathVariable Long id) {
        return ResponseEntity.ok(teacherService.getTeacherById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeacherResponse> updateTeacher(
            @PathVariable Long id,
            @RequestBody @Valid com.timetable.backend.domain.dto.UpdateTeacherRequest request) {
        return ResponseEntity.ok(teacherService.updateTeacher(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeacherResponse> createTeacher(@RequestBody @Valid CreateTeacherRequest request) {
        return ResponseEntity.ok(teacherService.createTeacher(request));
    }

    // ──────────────────────────────────────────────────────────
    // Teacher self-service: private lesson student pool
    // ──────────────────────────────────────────────────────────

    /**
     * Returns the list of students who have selected the authenticated teacher
     * for private lessons.
     */
    @GetMapping("/me/students")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<StudentResponse>> getMyStudents(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(teacherService.getMyStudents(email));
    }

    /**
     * GET /api/teachers/me/students/{studentId}/availability
     *
     * Returns the weekly availability of a student who has selected
     * the authenticated teacher for private lessons.
     *
     * Security: only accessible to the TEACHER whose student list contains studentId.
     * Returns 403 if student is not in the teacher's pool, 404 if student not found.
     */
    @GetMapping("/me/students/{studentId}/availability")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<StudentAvailabilityResponse> getStudentAvailability(
            @PathVariable Long studentId,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(teacherService.getStudentAvailability(email, studentId));
    }

    // ──────────────────────────────────────────────────────────
    // Teacher self-service: profile
    // ──────────────────────────────────────────────────────────

    /**
     * Returns the authenticated teacher's own profile.
     */
    @GetMapping("/me/profile")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TeacherResponse> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(teacherService.getMyProfile(authentication.getName()));
    }

    /**
     * Updates the authenticated teacher's own profile, including desiredLessonsPerWeek.
     */
    @PutMapping("/me/profile")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TeacherResponse> updateMyProfile(
            @RequestBody @Valid UpdateTeacherRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(teacherService.updateMyProfile(authentication.getName(), request));
    }

}

