package com.timetable.backend.service;

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

    // Repositories
    private final LessonRepository lessonRepository;
    private final TimeslotRepository timeslotRepository;
    private final RoomRepository roomRepository;
    private final TeacherRepository teacherRepository;
    private final ResourceUnavailabilityRepository resourceUnavailabilityRepository;

    /**
     * Loads the problem from database and starts solving asynchronously.
     *
     * @param scheduleId unique identifier for this solving session (can be any Long, e.g., timestamp)
     * @return the problem ID used to track solver status
     */
    public Long solve(Long scheduleId) {
        log.info("Starting solver for schedule ID: {}", scheduleId);

        // Start solving asynchronously using the new solveBuilder() pattern (Timefold 1.6.0+)
        // This replaces the deprecated solve() method
        solverManager.solveBuilder()
            .withProblemId(scheduleId)
            .withProblemFinder(this::loadProblemInternal)
            .withBestSolutionConsumer(this::saveSolution)
            .run();

        log.info("Solver started for schedule {}", scheduleId);

        return scheduleId;
    }

    /**
     * Internal method to load problem (to avoid @Transactional self-invocation issue).
     */
    @Transactional(readOnly = true)
    public DanceSchedule loadProblemInternal(Long scheduleId) {
        return loadProblem(scheduleId);
    }

    /**
     * Loads the planning problem from the database.
     * Creates a DanceSchedule with all problem facts and planning entities.
     *
     * @param scheduleId the schedule identifier
     * @return DanceSchedule ready for optimization
     */
    @Transactional(readOnly = true)
    public DanceSchedule loadProblem(Long scheduleId) {
        log.info("Loading problem data from database for schedule ID: {}", scheduleId);

        // Load all problem facts (immutable data)
        List<Timeslot> timeslots = timeslotRepository.findAll();
        List<Room> rooms = roomRepository.findAll();
        List<Teacher> teachers = teacherRepository.findAll();
        List<ResourceUnavailability> resourceUnavailabilities = resourceUnavailabilityRepository.findAll();

        // Load all planning entities (lessons to be scheduled)
        List<Lesson> lessons = lessonRepository.findAll();

        // Clear planning variables for non-pinned lessons
        // (Solver will assign timeslot and room)
        lessons.forEach(lesson -> {
            if (!lesson.isPinned()) {
                lesson.setTimeslot(null);
                lesson.setRoom(null);
            }
        });

        log.info("Loaded {} timeslots, {} rooms, {} teachers, {} lessons",
            timeslots.size(), rooms.size(), teachers.size(), lessons.size());

        // Create and return the planning problem
        return new DanceSchedule(
            scheduleId,
            timeslots,
            rooms,
            teachers,
            resourceUnavailabilities,
            lessons
        );
    }

    /**
     * Saves the optimized solution back to the database.
     * Updates timeslot and room assignments for all lessons.
     *
     * @param solution the solved DanceSchedule
     */
    @Transactional
    public void saveSolution(DanceSchedule solution) {
        log.info("Saving solution for schedule ID: {}, score: {}",
            solution.getId(), solution.getScore());

        if (solution.getScore() == null) {
            log.warn("Solution score is null, skipping save");
            return;
        }

        // Update lessons with assigned timeslots and rooms
        solution.getLessonList().forEach(lesson -> {
            log.info("Saving lesson {}: timeslot={}, room={}",
                lesson.getId(),
                lesson.getTimeslot() != null ? lesson.getTimeslot().getId() : "null",
                lesson.getRoom() != null ? lesson.getRoom().getId() : "null");

            Lesson persistedLesson = lessonRepository.findById(lesson.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Lesson not found: " + lesson.getId()));

            // Update planning variables
            persistedLesson.setTimeslot(lesson.getTimeslot());
            persistedLesson.setRoom(lesson.getRoom());

            lessonRepository.save(persistedLesson);
        });

        lessonRepository.flush();

        log.info("Successfully saved solution with {} lessons",
            solution.getLessonList().size());
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

    /**
     * Gets the current solution from database (solved or unsolved lessons).
     * This can be called at any time, even while solving is in progress.
     *
     * @param scheduleId the schedule identifier (not used, just for consistency)
     * @return current state of the schedule from database
     */
    @Transactional(readOnly = true)
    public DanceSchedule getCurrentSolutionFromDatabase(Long scheduleId) {
        return loadProblem(scheduleId);
    }

    /**
     * Checks if all lessons have been assigned timeslots and rooms.
     *
     * @param solution the DanceSchedule to check
     * @return true if all lessons are assigned
     */
    public boolean isFullyAssigned(DanceSchedule solution) {
        return solution.getLessonList().stream()
            .allMatch(lesson -> lesson.getTimeslot() != null && lesson.getRoom() != null);
    }
}

