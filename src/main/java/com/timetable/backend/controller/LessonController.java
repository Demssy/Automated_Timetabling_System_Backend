package com.timetable.backend.controller;

import com.timetable.backend.domain.dto.CreateLessonRequest;
import com.timetable.backend.domain.dto.ScheduledLessonDTO;
import com.timetable.backend.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ScheduledLessonDTO>> getAllLessons() {
        return ResponseEntity.ok(lessonService.getAllLessons());
    }


    @GetMapping("/active-schedule")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<ScheduledLessonDTO>> getActiveScheduleLessons() {
        return ResponseEntity.ok(lessonService.getActiveScheduleLessons());
    }

    /**
     * GET /api/lessons/my-schedule
     *
     * Returns scheduled lessons from the active PUBLISHED schedule that belong
     * to the currently authenticated user.
     * <ul>
     *   <li>STUDENT — their private lessons + group lessons they are enrolled in.</li>
     *   <li>TEACHER — all lessons (private and group) that they teach.</li>
     * </ul>
     * Data is sourced from the {@code scheduled_lessons} snapshot table only.
     */
    @GetMapping("/my-schedule")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ResponseEntity<List<ScheduledLessonDTO>> getMySchedule(Authentication authentication) {
        return ResponseEntity.ok(lessonService.getMySchedule(authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScheduledLessonDTO> getLessonById(@PathVariable Long id) {
        return ResponseEntity.ok(lessonService.getLessonById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScheduledLessonDTO> createLesson(@RequestBody @Valid CreateLessonRequest request) {
        return ResponseEntity.ok(lessonService.createLesson(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScheduledLessonDTO> updateLesson(
            @PathVariable Long id,
            @RequestBody @Valid CreateLessonRequest request) {
        return ResponseEntity.ok(lessonService.updateLesson(id, request));
    }

    @PutMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScheduledLessonDTO> toggleLessonActive(@PathVariable Long id) {
        return ResponseEntity.ok(lessonService.toggleLessonActive(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long id) {
        lessonService.deleteLesson(id);
        return ResponseEntity.noContent().build();
    }
}

