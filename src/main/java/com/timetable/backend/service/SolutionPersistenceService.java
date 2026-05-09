package com.timetable.backend.service;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timetable.backend.domain.dto.ConstraintViolationSummary;
import com.timetable.backend.domain.model.ScheduleMetadata;
import com.timetable.backend.domain.model.ScheduledLesson;
import com.timetable.backend.domain.model.ScheduledLessonStatus;
import com.timetable.backend.domain.repository.ScheduleMetadataRepository;
import com.timetable.backend.domain.repository.ScheduledLessonRepository;
import com.timetable.backend.solver.DanceSchedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SolutionPersistenceService {

    private final ScheduledLessonRepository scheduledLessonRepository;
    private final ScheduleMetadataRepository scheduleMetadataRepository;
    private final SolutionManager<DanceSchedule, HardSoftScore> solutionManager;
    private final ObjectMapper objectMapper;

    @Transactional
    public void saveSolution(DanceSchedule solution) {
        log.info("SolutionPersistenceService: Saving solution snapshot for schedule ID: {}, score: {}",
            solution.getId(), solution.getScore());

        if (solution.getId() == null) {
            throw new IllegalArgumentException("Schedule ID is required to persist solution snapshot.");
        }

        if (solution.getScore() == null) {
            log.warn("Solution score is null, skipping save");
            return;
        }

        ScheduleMetadata schedule = scheduleMetadataRepository.findById(solution.getId())
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + solution.getId()));

        // Build a lookup of already persisted snapshot rows for this schedule.
        var existingByLessonId = new HashMap<Long, ScheduledLesson>();
        scheduledLessonRepository.findByScheduleIdOrderByIdAsc(schedule.getId())
            .forEach(existing -> {
                if (existing.getLesson() != null && existing.getLesson().getId() != null) {
                    existingByLessonId.put(existing.getLesson().getId(), existing);
                }
            });

        var snapshotsToSave = new ArrayList<ScheduledLesson>(solution.getLessonList().size());
        var incomingLessonIds = new HashSet<Long>();

        solution.getLessonList().forEach(lesson -> {
            if (lesson.getId() == null) {
                throw new IllegalArgumentException("Lesson ID is required for schedule snapshot persistence.");
            }

            // Skip unmatched private lessons (no student assigned).
            // By NOT adding these to incomingLessonIds, any previously persisted snapshot
            // row for this lesson will be treated as stale and automatically deleted below.
            if (lesson.isPrivate() && lesson.getStudent() == null) {
                return;
            }

            // Skip duplicate lessons from solver output to avoid duplicate rows.
            if (!incomingLessonIds.add(lesson.getId())) {
                log.warn("Duplicate lesson {} detected in solver output for schedule {}. Skipping duplicate row.",
                    lesson.getId(), schedule.getId());
                return;
            }

            var status = lesson.getTimeslot() != null
                ? ScheduledLessonStatus.ASSIGNED
                : ScheduledLessonStatus.UNASSIGNED;

            var existing = existingByLessonId.get(lesson.getId());
            if (existing != null) {
                // Never overwrite a manually cancelled lesson — the solver must not undo
                // a human cancellation decision. Keep the row in incomingLessonIds so that
                // the stale-snapshot cleanup loop does NOT delete it.
                if (existing.isCancelled()) {
                    incomingLessonIds.add(lesson.getId());
                    return;
                }
                existing.setTimeslot(lesson.getTimeslot());
                existing.setRoom(lesson.getRoom());
                existing.setStudent(lesson.getStudent());
                existing.setStatus(status);
                snapshotsToSave.add(existing);
                return;
            }

            snapshotsToSave.add(new ScheduledLesson(
                null,
                lesson,
                schedule,
                lesson.getTimeslot(),
                status,
                lesson.getRoom(),
                lesson.getStudent(),
                false,
                null,
                null,
                null
            ));
        });

        // Remove stale snapshot rows that are no longer present in the incoming solution.
        var staleSnapshots = existingByLessonId.values().stream()
            .filter(existing -> !incomingLessonIds.contains(existing.getLesson().getId()))
            .toList();
        if (!staleSnapshots.isEmpty()) {
            scheduledLessonRepository.deleteAll(staleSnapshots);
        }

        scheduledLessonRepository.saveAll(snapshotsToSave);

        schedule.setSolverScore(solution.getScore().toString());
        schedule.setScoreExplanation(buildScoreExplanationJson(solution));
        scheduleMetadataRepository.save(schedule);

        log.info("Successfully saved {} snapshot lessons for schedule {}, score={}",
            snapshotsToSave.size(), schedule.getId(), schedule.getSolverScore());
    }

    /**
     * Uses Timefold {@link SolutionManager#explain(Object)} to obtain per-constraint
     * score contributions and serializes them to a JSON string for storage.
     *
     * <p>Only constraints with a non-zero score are included.
     * The list is sorted: hard violations first (ascending by hardScore),
     * then soft contributions.</p>
     *
     * @param solution the best solution returned by the solver
     * @return JSON array string, e.g. {@code [{"constraintName":"Teacher conflict","hardScore":-1,...}]}
     */
    private String buildScoreExplanationJson(DanceSchedule solution) {
        try {
            var explanation = solutionManager.explain(solution);

            List<ConstraintViolationSummary> summaries = explanation
                .getConstraintMatchTotalMap()
                .values()
                .stream()
                .filter(cmt -> {
                    var s = (HardSoftScore) cmt.getScore();
                    return s.hardScore() != 0 || s.softScore() != 0;
                })
                .map(cmt -> {
                    var s = (HardSoftScore) cmt.getScore();
                    return new ConstraintViolationSummary(
                        cmt.getConstraintRef().constraintName(),
                        s.hardScore(),
                        s.softScore(),
                        cmt.getConstraintMatchCount()
                    );
                })
                // Hard violations first (most negative first), then soft
                .sorted(Comparator
                    .comparingInt(ConstraintViolationSummary::hardScore)
                    .thenComparingInt(ConstraintViolationSummary::softScore))
                .toList();

            return objectMapper.writeValueAsString(summaries);

        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize score explanation for schedule {}", solution.getId(), e);
            return "[]";
        } catch (Exception e) {
            log.warn("SolutionManager.explain() failed for schedule {}: {}", solution.getId(), e.getMessage());
            return "[]";
        }
    }
}
