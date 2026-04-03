package com.timetable.backend.service;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
import com.timetable.backend.domain.model.*;
import com.timetable.backend.domain.repository.*;
import com.timetable.backend.solver.DanceSchedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing Timefold Solver operations.
 * Handles asynchronous schedule optimization and result persistence.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SolverService {

    private final SolverManager<DanceSchedule, Long> solverManager;
    private final SolverProblemLoaderService problemLoaderService;

    private final ScheduledLessonRepository scheduledLessonRepository;
    private final ScheduleMetadataRepository scheduleMetadataRepository;
    private final SolutionPersistenceService persistenceService;
    /**
     * Loads the problem from database and starts solving asynchronously.
     *
     * @param scheduleId unique identifier for this solving session (can be any Long, e.g., timestamp)
     */
    public void solve(Long scheduleId) {
        log.info("Starting solver for schedule ID: {}", scheduleId);

        solverManager.solveBuilder()
                .withProblemId(scheduleId)
                .withProblemFinder(problemLoaderService::loadProblem)
                .withBestSolutionConsumer(persistenceService::saveSolution)
                .run();
    }

    /**
     * Gets the current status of the solver for a given schedule.
     *
     * @param scheduleId the schedule identifier
     * @return SolverStatus (NOT_SOLVING, SOLVING_SCHEDULED, SOLVING_ACTIVE)
     */
    public SolverStatus getSolverStatus(Long scheduleId) {
        return solverManager.getSolverStatus(scheduleId);
    }

    /**
     * Terminates solving early for a given schedule.
     * The best solution found so far will be saved.
     *
     * @param scheduleId the schedule identifier
     * @return true if termination was successful
     */
    public boolean terminateEarly(Long scheduleId) {
        log.info("Terminating solver early for schedule ID: {}", scheduleId);

        SolverStatus status = solverManager.getSolverStatus(scheduleId);

        if (status == SolverStatus.NOT_SOLVING) {
            log.warn("Cannot terminate - solver is not running for schedule {}", scheduleId);
            return false;
        }

        solverManager.terminateEarly(scheduleId);
        log.info("Early termination requested for schedule {}", scheduleId);
        return true;
    }

    /**
     * Retrieves the best solution found so far (blocking call).
     * Only use this for testing or when you need the solution immediately.
     *
     * @param scheduleId the schedule identifier
     * @return the best solution found, or null if not available
     */
    @SuppressWarnings("unused")
    public DanceSchedule getBestSolution(Long scheduleId) {
        SolverStatus status = solverManager.getSolverStatus(scheduleId);

        if (status == SolverStatus.NOT_SOLVING) {
            log.warn("Solver is not running for schedule {}", scheduleId);
            return null;
        }

        // For active/scheduled solving, we can't easily get intermediate results in 1.6.0
        // Best practice is to wait for completion and use saved solution from DB
        log.info("Solver is still running for schedule {}. Status: {}", scheduleId, status);
        return null;
    }


    @Transactional(readOnly = true)
    public List<ScheduledLesson> getCurrentSolutionFromDatabase(Long scheduleId) {
        return scheduledLessonRepository.findByScheduleIdOrderByIdAsc(scheduleId);
    }

    @Transactional(readOnly = true)
    public HardSoftScore getStoredScore(Long scheduleId) {
        return scheduleMetadataRepository.findById(scheduleId)
            .map(ScheduleMetadata::getSolverScore)
            .filter(score -> !score.isBlank())
            .map(score -> {
                try {
                    return HardSoftScore.parseScore(score);
                } catch (IllegalArgumentException e) {
                    log.warn("Cannot parse stored solver score '{}' for schedule {}", score, scheduleId);
                    return null;
                }
            })
            .orElse(null);
    }

    /**
     * Checks if all lessons have been assigned timeslots and rooms.
     *
     * @param scheduledLessons the list of ScheduledLesson to check
     * @return true if all lessons are assigned
     */
    public boolean isFullyAssigned(List<ScheduledLesson> scheduledLessons) {
        return scheduledLessons.stream()
            .allMatch(lesson -> lesson.getStatus() == ScheduledLessonStatus.ASSIGNED);
    }
}
