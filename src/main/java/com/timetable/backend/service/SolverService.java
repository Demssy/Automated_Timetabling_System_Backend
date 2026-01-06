package com.timetable.backend.service;

import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
import com.timetable.backend.domain.model.*;
import com.timetable.backend.domain.repository.*;
import com.timetable.backend.solver.domain.TimetableSolution;
import com.timetable.backend.solver.mapper.PlanningModelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of solver service for Timefold Solver operations.
 * Handles asynchronous schedule optimization and result persistence.
 *
 * REFACTORED: Now uses Planning Model (Pure POJOs) instead of JPA entities.
 * This eliminates Hibernate overhead and N+1 query risks during solving.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SolverService implements ISolverService {

    private final SolverManager<TimetableSolution, Long> solverManager;
    private final PlanningModelMapper planningMapper;

    // Repositories
    private final LessonRepository lessonRepository;
    private final TimeslotRepository timeslotRepository;
    private final RoomRepository roomRepository;
    private final TeacherRepository teacherRepository;
    private final ResourceUnavailabilityRepository resourceUnavailabilityRepository;

    /**
     * Loads the problem from database and starts solving asynchronously.
     *
     * REFACTORED: Now converts JPA entities to Planning POJOs before solving.
     * This ensures zero Hibernate overhead during optimization.
     *
     * @param scheduleId unique identifier for this solving session (can be any Long, e.g., timestamp)
     */
    public void solve(Long scheduleId) {
        log.info("Starting solver for schedule ID: {}", scheduleId);

        // Start solving asynchronously using the new solveBuilder() pattern (Timefold 1.6.0+)
        // This replaces the deprecated solve() method
        solverManager.solveBuilder()
            .withProblemId(scheduleId)
            .withProblemFinder(this::loadProblemInternal)
            .withBestSolutionConsumer(this::saveSolution)
            .run();

        log.info("Solver started for schedule {}", scheduleId);
    }

    /**
     * Internal method to load problem (to avoid @Transactional self-invocation issue).
     */
    @Transactional(readOnly = true)
    public TimetableSolution loadProblemInternal(Long scheduleId) {
        return loadProblem(scheduleId);
    }

    /**
     * Loads the planning problem from the database and converts to Planning Model.
     *
     * REFACTORED: Uses PlanningModelMapper to convert JPA entities to lightweight POJOs.
     * This eliminates Hibernate proxies and ensures fast cloning during solving.
     *
     * @param scheduleId the schedule identifier
     * @return TimetableSolution ready for optimization (Planning POJOs, not JPA entities)
     */
    @Transactional(readOnly = true)
    public TimetableSolution loadProblem(Long scheduleId) {
        log.info("Loading problem data from database for schedule ID: {}", scheduleId);

        // 1. Load JPA entities from database (with eager fetching to avoid LazyInit)
        List<Timeslot> timeslots = timeslotRepository.findAll();
        List<Room> rooms = roomRepository.findAll();
        List<Teacher> teachers = teacherRepository.findAll();
        List<ResourceUnavailability> unavailabilities = resourceUnavailabilityRepository.findAll();
        List<Lesson> lessons = lessonRepository.findAll();

        log.info("Loaded {} timeslots, {} rooms, {} teachers, {} lessons",
            timeslots.size(), rooms.size(), teachers.size(), lessons.size());

        // 2. Convert JPA entities to Planning POJOs via mapper
        // This unproxies Hibernate entities and creates lightweight clones
        TimetableSolution solution = planningMapper.toPlanningSolution(
            scheduleId,
            lessons,
            timeslots,
            rooms,
            teachers,
            unavailabilities
        );

        // 3. Clear planning variables for non-pinned lessons
        // (Solver will assign timeslot and room during optimization)
        solution.getLessonList().forEach(planningLesson -> {
            if (!planningLesson.isPinned()) {
                planningLesson.setTimeslot(null);
                planningLesson.setRoom(null);
            }
        });

        return solution;
    }

    /**
     * Saves the optimized solution back to the database.
     *
     * REFACTORED: Converts Planning POJOs back to JPA entities via mapper.
     * Only updates timeslot and room assignments (planning variables).
     *
     * @param solution the solved TimetableSolution (Planning Model)
     */
    @Transactional
    public void saveSolution(TimetableSolution solution) {
        log.info("Saving solution for schedule ID: {}, score: {}",
            solution.getId(), solution.getScore());

        if (solution.getScore() == null) {
            log.warn("Solution score is null, skipping save");
            return;
        }

        // 1. Create lookup maps for timeslots and rooms (for mapper)
        Map<Long, Timeslot> timeslotMap = timeslotRepository.findAll().stream()
            .collect(Collectors.toMap(Timeslot::getId, t -> t));
        Map<Long, Room> roomMap = roomRepository.findAll().stream()
            .collect(Collectors.toMap(Room::getId, r -> r));

        // 2. Convert Planning Model to LessonUpdate DTOs via mapper
        var updates = planningMapper.toPersistableLessons(solution, timeslotMap, roomMap);

        // 3. Apply updates to JPA entities and persist
        updates.forEach(update -> {
            log.info("Saving lesson {}: timeslot={}, room={}",
                update.lessonId(),
                update.timeslotId(),
                update.roomId());

            Lesson lesson = lessonRepository.findById(update.lessonId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Lesson not found: " + update.lessonId()));

            // Update planning variables (timeslot and room assignments)
            lesson.setTimeslot(update.timeslotId() != null
                ? timeslotMap.get(update.timeslotId())
                : null);
            lesson.setRoom(update.roomId() != null
                ? roomMap.get(update.roomId())
                : null);
        });

        lessonRepository.flush();

        log.info("Successfully saved solution with {} lessons", updates.size());
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
    @Override
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
    public TimetableSolution getBestSolution(Long scheduleId) {
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
     * Note: This method does NOT clear planning variables, so you can see assigned timeslots and rooms.
     *
     * @param scheduleId the schedule identifier
     * @return current state of the schedule from database as TimetableSolution
     */
    @Override
    @Transactional(readOnly = true)
    public TimetableSolution getCurrentSolutionFromDatabase(Long scheduleId) {
        log.info("Loading current solution from database for schedule ID: {}", scheduleId);

        // Load JPA entities
        List<Timeslot> timeslots = timeslotRepository.findAll();
        List<Room> rooms = roomRepository.findAll();
        List<Teacher> teachers = teacherRepository.findAll();
        List<ResourceUnavailability> unavailabilities = resourceUnavailabilityRepository.findAll();
        List<Lesson> lessons = lessonRepository.findAll();

        // Convert to Planning Model (includes current assignments)
        return planningMapper.toPlanningSolution(
            scheduleId,
            lessons,
            timeslots,
            rooms,
            teachers,
            unavailabilities
        );
    }

    /**
     * Checks if all lessons have been assigned timeslots and rooms.
     *
     * @param solution the TimetableSolution to check
     * @return true if all lessons are assigned
     */
    public boolean isFullyAssigned(TimetableSolution solution) {
        return solution.getLessonList().stream()
            .allMatch(lesson -> lesson.getTimeslot() != null && lesson.getRoom() != null);
    }
}

