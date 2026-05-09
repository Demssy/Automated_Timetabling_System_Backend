package com.timetable.backend.controller;

import com.timetable.backend.domain.dto.CancelLessonRequest;
import com.timetable.backend.domain.dto.ScheduledLessonDTO;
import com.timetable.backend.service.ScheduledLessonCancellationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing scheduled lesson state.
 * Base path: /api/scheduled-lessons
 *
 * <p>Cancellation endpoints are intentionally separated from {@code /api/lessons}
 * because they operate on the {@code scheduled_lessons} snapshot table, not the
 * lesson template table.
 */
@RestController
@RequestMapping("/api/scheduled-lessons")
@RequiredArgsConstructor
public class ScheduledLessonController {

    private final ScheduledLessonCancellationService cancellationService;

    /**
     * PATCH /api/scheduled-lessons/{id}/cancel
     *
     * <p>Cancels a specific scheduled lesson. Teachers may only cancel their own lessons;
     * admins may cancel any lesson.
     *
     * @param id             the ScheduledLesson ID (from the snapshot table)
     * @param request        optional body with a cancellation reason
     * @param authentication injected Spring Security context
     * @return the updated {@link ScheduledLessonDTO} with {@code isCancelled=true}
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ScheduledLessonDTO> cancelLesson(
            @PathVariable Long id,
            @RequestBody(required = false) CancelLessonRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(cancellationService.cancelLesson(id, request, authentication));
    }

    /**
     * PATCH /api/scheduled-lessons/{id}/restore
     *
     * <p>Reverts a previously cancelled lesson back to its original state.
     * Restricted to ADMIN only (teachers cannot undo cancellations).
     *
     * @param id             the ScheduledLesson ID (from the snapshot table)
     * @param authentication injected Spring Security context
     * @return the updated {@link ScheduledLessonDTO} with {@code isCancelled=false}
     */
    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScheduledLessonDTO> restoreLesson(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(cancellationService.restoreLesson(id, authentication));
    }
}

