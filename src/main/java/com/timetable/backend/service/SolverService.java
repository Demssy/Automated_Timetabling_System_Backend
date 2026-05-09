package com.timetable.backend.service;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timetable.backend.domain.dto.ConstraintViolationSummary;
import com.timetable.backend.domain.dto.ScoreExplanationResponse;
import com.timetable.backend.domain.dto.UnmetStudentDTO;
import com.timetable.backend.domain.model.*;
import com.timetable.backend.domain.repository.*;
import com.timetable.backend.solver.DanceSchedule;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final ObjectMapper objectMapper;
    private final WeeklyAvailabilityRepository weeklyAvailabilityRepository;
    private final TeacherRepository teacherRepository;

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
     * Checks if all lessons have been assigned timeslots.
     *
     * @param scheduledLessons the list of ScheduledLesson to check
     * @return true if all lessons are assigned
     */
    public boolean isFullyAssigned(List<ScheduledLesson> scheduledLessons) {
        if (scheduledLessons.isEmpty()) {
            return false; // Empty schedule is not "fully assigned"
        }
        return scheduledLessons.stream()
                .allMatch(lesson -> lesson.getStatus() == ScheduledLessonStatus.ASSIGNED);
    }

    /**
     * Returns the per-constraint score explanation for a solved schedule.
     * The explanation was computed by {@link SolutionPersistenceService} using
     * {@code SolutionManager.explain()} and stored as JSON in {@code schedule_metadata}.
     *
     * @param scheduleId the schedule identifier
     * @return {@link ScoreExplanationResponse} with total score and per-constraint breakdown
     * @throws EntityNotFoundException if no schedule with the given ID exists
     */
    @Transactional(readOnly = true)
    public ScoreExplanationResponse getScoreExplanation(Long scheduleId) {
        ScheduleMetadata meta = scheduleMetadataRepository.findById(scheduleId)
            .orElseThrow(() -> new EntityNotFoundException("Schedule not found: " + scheduleId));

        List<ConstraintViolationSummary> violations = parseViolations(meta.getScoreExplanation());

        return new ScoreExplanationResponse(scheduleId, meta.getSolverScore(), violations);
    }

    /**
     * Deserializes the stored JSON explanation back into a list of summaries.
     * Returns an empty list if the column is null, blank, or malformed.
     */
    private List<ConstraintViolationSummary> parseViolations(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json,
                new TypeReference<List<ConstraintViolationSummary>>() {});
        } catch (Exception e) {
            log.warn("Cannot parse stored score explanation: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Returns students who received fewer lessons than their declared
     * weekly availability windows suggest they wanted.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Load all teachers and collect their subscribed students into a unique set.</li>
     *   <li>For each student count their {@code WeeklyAvailability} rows = desired slots.</li>
     *   <li>Count their {@code ScheduledLesson} rows for this schedule = assigned lessons.</li>
     *   <li>Return students where assigned &lt; desired, sorted by missing count descending.</li>
     * </ol>
     * </p>
     *
     * @param scheduleId the schedule identifier
     * @return list of students with unmet lesson demand, sorted by most-missed first
     */
    @Transactional(readOnly = true)
    public List<UnmetStudentDTO> getUnmetStudents(Long scheduleId) {
        // Step 1: build studentId -> assignedLessons map from schedule snapshot
        Map<Long, Integer> assignedByStudent = scheduledLessonRepository
            .countAssignedLessonsByStudent(scheduleId)
            .stream()
            .collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> ((Number) row[1]).intValue()
            ));

        // Step 2: collect all unique students subscribed to at least one teacher
        Set<Student> allSubscribedStudents = teacherRepository.findAll()
            .stream()
            .flatMap(teacher -> teacher.getPrivateStudents().stream())
            .collect(Collectors.toSet());

        // Step 3: compare desired (availability windows) vs assigned for each student
        return allSubscribedStudents.stream()
            .map(student -> {
                int desired = weeklyAvailabilityRepository.findByUserId(student.getId()).size();
                int assigned = assignedByStudent.getOrDefault(student.getId(), 0);
                int missing = desired - assigned;
                return new UnmetStudentDTO(
                    student.getId(),
                    student.getFullName(),
                    student.getEmail(),
                    desired,
                    assigned,
                    missing
                );
            })
            .filter(dto -> dto.missingLessons() > 0)
            .sorted(Comparator.comparingInt(UnmetStudentDTO::missingLessons).reversed())
            .toList();
    }
}
