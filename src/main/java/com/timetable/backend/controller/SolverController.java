package com.timetable.backend.controller;

import ai.timefold.solver.core.api.solver.SolverStatus;
import com.timetable.backend.domain.dto.*;
import com.timetable.backend.domain.model.Lesson;
import com.timetable.backend.service.SolverService;
import com.timetable.backend.solver.DanceSchedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for Timefold Solver operations.
 * Provides endpoints for schedule optimization and monitoring.
 */
@RestController
@RequestMapping("/api/solver")
@RequiredArgsConstructor
@Slf4j
public class SolverController {

    private final SolverService solverService;

    /**
     * Starts the solver to optimize the schedule.
     * The solving process runs asynchronously.
     *
     * POST /api/solver/solve
     *
     * @return 202 Accepted with schedule ID for tracking
     */
    @PostMapping("/solve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SolveResponse> solve() {
        log.info("Received request to start schedule optimization");

        // Use current timestamp as schedule ID
        Long scheduleId = System.currentTimeMillis();

        try {
            solverService.solve(scheduleId);

            return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(SolveResponse.started(scheduleId));

        } catch (Exception e) {
            log.error("Error starting solver", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }

    /**
     * Gets the current status of the solver for a given schedule.
     *
     * GET /api/solver/status/{scheduleId}
     *
     * @param scheduleId the schedule identifier
     * @return solver status (NOT_SOLVING, SOLVING_SCHEDULED, SOLVING_ACTIVE)
     */
    @GetMapping("/status/{scheduleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SolverStatusResponse> getStatus(@PathVariable Long scheduleId) {
        log.info("Checking solver status for schedule ID: {}", scheduleId);

        try {
            SolverStatus status = solverService.getSolverStatus(scheduleId);

            return ResponseEntity.ok(
                SolverStatusResponse.of(scheduleId, status)
            );

        } catch (Exception e) {
            log.error("Error retrieving solver status", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }

    /**
     * Terminates the solver early for a given schedule.
     * The best solution found so far will be saved.
     *
     * POST /api/solver/terminate/{scheduleId}
     *
     * @param scheduleId the schedule identifier
     * @return 200 OK if termination successful, 400 if not running
     */
    @PostMapping("/terminate/{scheduleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> terminateEarly(@PathVariable Long scheduleId) {
        log.info("Received request to terminate solver for schedule ID: {}", scheduleId);

        try {
            boolean terminated = solverService.terminateEarly(scheduleId);

            if (terminated) {
                return ResponseEntity.ok(
                    "Solver termination requested for schedule " + scheduleId
                );
            } else {
                return ResponseEntity
                    .badRequest()
                    .body("Solver is not running for schedule " + scheduleId);
            }

        } catch (Exception e) {
            log.error("Error terminating solver", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error terminating solver: " + e.getMessage());
        }
    }

    /**
     * Retrieves the current solution from database.
     * Shows the current state of lessons (solved or unsolved).
     *
     * GET /api/solver/solution/{scheduleId}
     *
     * @param scheduleId the schedule identifier
     * @return the current solution from database
     */
    @GetMapping("/solution/{scheduleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScheduleSolutionResponse> getSolution(@PathVariable Long scheduleId) {
        log.info("Retrieving current solution from database for schedule ID: {}", scheduleId);

        try {
            DanceSchedule solution = solverService.getCurrentSolutionFromDatabase(scheduleId);

            if (solution == null) {
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .build();
            }

            // Map lessons to DTOs
            List<ScheduledLessonDTO> lessonDTOs = solution.getLessonList().stream()
                .map(this::mapToScheduledLessonDTO)
                .collect(Collectors.toList());

            boolean fullyAssigned = solverService.isFullyAssigned(solution);

            ScheduleSolutionResponse response = ScheduleSolutionResponse.from(
                scheduleId,
                solution.getScore(),
                fullyAssigned,
                lessonDTOs
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving solution", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
        }
    }

    /**
     * Helper method to map Lesson entity to ScheduledLessonDTO.
     */
    private ScheduledLessonDTO mapToScheduledLessonDTO(Lesson lesson) {
        return new ScheduledLessonDTO(
            lesson.getId(),
            lesson.getTeacher() != null ? lesson.getTeacher().getFullName() : "N/A",
            lesson.getDanceGroup() != null ? lesson.getDanceGroup().getName() : "N/A",
            lesson.getTimeslot() != null ? lesson.getTimeslot().getDayOfWeek() : null,
            lesson.getTimeslot() != null ? lesson.getTimeslot().getStartTime() : null,
            lesson.getTimeslot() != null ? lesson.getTimeslot().getEndTime() : null,
            lesson.getRoom() != null ? lesson.getRoom().getName() : "Unassigned",
            lesson.getDurationMinutes(),
            lesson.isPrivate(),
            lesson.isPinned()
        );
    }
}

