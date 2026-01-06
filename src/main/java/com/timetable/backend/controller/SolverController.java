package com.timetable.backend.controller;

import ai.timefold.solver.core.api.solver.SolverStatus;
import com.timetable.backend.domain.dto.*;
import com.timetable.backend.domain.mapper.LessonMapper;
import com.timetable.backend.service.SolverService;
import com.timetable.backend.solver.domain.TimetableSolution;
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
 *
 * REFACTORED: Updated to work with TimetableSolution (Planning Model).
 */
@RestController
@RequestMapping("/api/v1/solver")
@RequiredArgsConstructor
@Slf4j
public class SolverController {

    private final SolverService solverService;
    private final LessonMapper lessonMapper;

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
            TimetableSolution solution = solverService.getCurrentSolutionFromDatabase(scheduleId);

            if (solution == null) {
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .build();
            }

            // Map Planning lessons to DTOs
            List<ScheduledLessonDTO> lessonDTOs = solution.getLessonList().stream()
                .map(planningLesson -> new ScheduledLessonDTO(
                    planningLesson.getId(),
                    planningLesson.getTeacher() != null ? planningLesson.getTeacher().getFullName() : null,
                    planningLesson.getDanceGroup() != null ? planningLesson.getDanceGroup().getName() : null,
                    planningLesson.getTimeslot() != null ? planningLesson.getTimeslot().getDayOfWeek() : null,
                    planningLesson.getTimeslot() != null ? planningLesson.getTimeslot().getStartTime() : null,
                    planningLesson.getTimeslot() != null ? planningLesson.getTimeslot().getEndTime() : null,
                    planningLesson.getRoom() != null ? planningLesson.getRoom().getName() : null,
                    planningLesson.getDurationMinutes(),
                    planningLesson.isPrivate(),
                    planningLesson.isPinned()
                ))
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
}

