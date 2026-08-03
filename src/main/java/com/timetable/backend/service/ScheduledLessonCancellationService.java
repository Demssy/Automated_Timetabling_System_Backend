package com.timetable.backend.service;

import com.timetable.backend.domain.dto.CancelLessonRequest;
import com.timetable.backend.domain.dto.ScheduledLessonDTO;
import com.timetable.backend.domain.mapper.LessonMapper;
import com.timetable.backend.domain.model.AbstractUser;
import com.timetable.backend.domain.model.ScheduledLesson;
import com.timetable.backend.domain.repository.ScheduledLessonRepository;
import com.timetable.backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * Handles manual cancellation and restoration of scheduled lessons by teachers and admins.
 * <p>
 * Cancellation is stored as a separate boolean flag ({@code is_cancelled}) on the
 * {@link ScheduledLesson} entity and is intentionally kept apart from the solver-managed
 * {@code status} column so that solver re-runs never silently undo a cancellation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledLessonCancellationService {

    private final ScheduledLessonRepository scheduledLessonRepository;
    private final UserRepository userRepository;
    private final LessonMapper lessonMapper;

    /**
     * Marks a scheduled lesson as cancelled.
     *
     * <p>Business rules:
     * <ul>
     *   <li>Only TEACHER and ADMIN roles may call this (enforced at controller level).</li>
     *   <li>A TEACHER may only cancel lessons that belong to their own schedule.</li>
     *   <li>Cannot cancel an already-cancelled lesson.</li>
     * </ul>
     *
     * @param scheduledLessonId ID of the {@link ScheduledLesson} to cancel
     * @param request           optional cancel reason
     * @param authentication    Spring Security context of the caller
     * @return updated {@link ScheduledLessonDTO}
     */
    @Transactional
    public ScheduledLessonDTO cancelLesson(Long scheduledLessonId,
                                           CancelLessonRequest request,
                                           Authentication authentication) {
        ScheduledLesson scheduledLesson = findOrThrow(scheduledLessonId);

        if (scheduledLesson.isCancelled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Lesson is already cancelled (id=" + scheduledLessonId + ")");
        }

        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");
        if (!isAdmin) {
            verifyTeacherOwnership(scheduledLesson, authentication);
        }

        AbstractUser caller = resolveUser(authentication);

        scheduledLesson.setCancelled(true);
        scheduledLesson.setCancelledBy(caller);
        scheduledLesson.setCancelledAt(LocalDateTime.now());
        scheduledLesson.setCancelReason(request != null ? request.reason() : null);

        log.info("Lesson (scheduledId={}) cancelled by user {} at {}",
                scheduledLessonId, caller.getId(), scheduledLesson.getCancelledAt());

        return lessonMapper.toScheduledLessonDTO(scheduledLessonRepository.save(scheduledLesson));
    }

    /**
     * Restores a previously cancelled lesson (undo cancellation).
     * Only ADMIN role is allowed to restore lessons.
     *
     * @param scheduledLessonId ID of the {@link ScheduledLesson} to restore
     * @param authentication    Spring Security context of the caller
     * @return updated {@link ScheduledLessonDTO}
     */
    @Transactional
    public ScheduledLessonDTO restoreLesson(Long scheduledLessonId, Authentication authentication) {
        ScheduledLesson scheduledLesson = findOrThrow(scheduledLessonId);

        if (!scheduledLesson.isCancelled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Lesson is not cancelled, nothing to restore (id=" + scheduledLessonId + ")");
        }

        AbstractUser caller = resolveUser(authentication);

        scheduledLesson.setCancelled(false);
        scheduledLesson.setCancelledBy(null);
        scheduledLesson.setCancelledAt(null);
        scheduledLesson.setCancelReason(null);

        log.info("Lesson (scheduledId={}) restored by admin user {}", scheduledLessonId, caller.getId());

        return lessonMapper.toScheduledLessonDTO(scheduledLessonRepository.save(scheduledLesson));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private ScheduledLesson findOrThrow(Long id) {
        return scheduledLessonRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Scheduled lesson not found: " + id));
    }

    private AbstractUser resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Authenticated user not found: " + authentication.getName()));
    }

    /**
     * Ensures a teacher can only cancel their own lessons.
     * Teacher uses composition (not inheritance): email is accessed via {@code teacher.getUser().getEmail()}.
     * Throws 403 FORBIDDEN if the lesson belongs to a different teacher.
     */
    private void verifyTeacherOwnership(ScheduledLesson scheduledLesson, Authentication authentication) {
        String callerEmail = authentication.getName();
        var sourceTeacher = scheduledLesson.getSourceTeacher();
        if (sourceTeacher == null || sourceTeacher.getUser() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Scheduled lesson has no teacher source");
        }
        // Teacher uses @MapsId composition — email lives on the nested AbstractUser, not Teacher itself.
        String lessonTeacherEmail = sourceTeacher.getUser().getEmail();

        if (!callerEmail.equals(lessonTeacherEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Teachers may only cancel their own lessons");
        }
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }
}

