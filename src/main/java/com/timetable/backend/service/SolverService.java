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
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final SolutionManager<DanceSchedule, HardSoftScore> solutionManager;
    private final SolutionPersistenceService persistenceService;
    /**
     * Loads the problem from database and starts solving asynchronously.
     *
     * @param scheduleId unique identifier for this solving session (can be any Long, e.g., timestamp)
     */

    @Lazy
    @Autowired
    private SolverService self;

    public void solve(Long scheduleId) {
        log.info("Starting solver for schedule ID: {}", scheduleId);

        solverManager.solveBuilder()
                .withProblemId(scheduleId)
                // Call through Spring proxy so @Transactional on loadProblem is applied.
                .withProblemFinder(id -> self.loadProblem(id))
                .withBestSolutionConsumer(persistenceService::saveSolution)
                .run();
    }

    @Transactional(readOnly = true)
    public DanceSchedule loadProblem(Long scheduleId) {
        DanceSchedule schedule = loadScheduleFromDatabase(scheduleId);

        // Clear planning variables for non-pinned lessons
        // (Solver will assign timeslot and room)
        schedule.getLessonList().forEach(lesson -> {
            if (!lesson.isPinned()) {
                lesson.setTimeslot(null);
                lesson.setRoom(null);
            }
        });

        return schedule;
    }

    /**
     * Private helper method to load schedule data from database.
     * Extracts common data loading logic to avoid code duplication.
     *
     * @param scheduleId the schedule identifier
     * @return DanceSchedule with all data loaded from database
     */
    private DanceSchedule loadScheduleFromDatabase(Long scheduleId) {
        log.info("Loading problem data from database for schedule ID: {}", scheduleId);

        // Load all problem facts (immutable data)
        List<Timeslot> timeslots = timeslotRepository.findAll();
        List<Room> rooms = roomRepository.findAll();
        List<Teacher> teachers = teacherRepository.findAll();
        List<ResourceUnavailability> resourceUnavailabilities = resourceUnavailabilityRepository.findAll();

        // Load planning entities only for the selected schedule.
        List<Lesson> lessons = lessonRepository.findByScheduleId(scheduleId);


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
    public DanceSchedule getCurrentSolutionFromDatabase(Long scheduleId) {
        DanceSchedule schedule = loadScheduleFromDatabase(scheduleId);
        solutionManager.update(schedule);
        return schedule;
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

