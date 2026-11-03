package com.timetable.backend.service;

import com.timetable.backend.domain.model.ScheduleMetadata;
import com.timetable.backend.domain.model.ScheduledLesson;
import com.timetable.backend.domain.model.ScheduledLessonStatus;
import com.timetable.backend.domain.model.ScheduleStatus;
import com.timetable.backend.domain.repository.ScheduleMetadataRepository;
import com.timetable.backend.domain.repository.ScheduledLessonRepository;
import com.timetable.backend.solver.DanceSchedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
@Slf4j
public class SolutionPersistenceService {

    private final ScheduledLessonRepository scheduledLessonRepository;
    private final ScheduleMetadataRepository scheduleMetadataRepository;
    private final ScheduleScoreEvaluationService scoreEvaluationService;

    @Transactional
    public void saveSolution(DanceSchedule solution, long revision) {
        log.info("SolutionPersistenceService: Saving solution snapshot for schedule ID: {}, score: {}",
            solution.getId(), solution.getScore());

        if (solution.getId() == null) {
            throw new IllegalArgumentException("Schedule ID is required to persist solution snapshot.");
        }

        if (solution.getScore() == null) {
            throw new IllegalArgumentException("Solution score is required to persist a schedule");
        }

        ScheduleMetadata schedule = scheduleMetadataRepository.findByIdForUpdate(solution.getId())
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + solution.getId()));

        if (schedule.getStatus() != ScheduleStatus.DRAFT || schedule.getSolverRevision() != revision) {
            log.info("Discarding stale solver result for schedule {}, revision {}", solution.getId(), revision);
            return;
        }

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

            var existing = existingByLessonId.get(lesson.getId());
            if (existing != null && existing.isCancelled()) {
                incomingLessonIds.add(lesson.getId());
                return;
            }

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

            if (existing != null) {
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
                null,
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
            .filter(existing -> !existing.isCancelled()
                && !incomingLessonIds.contains(existing.getLesson().getId()))
            .toList();
        if (!staleSnapshots.isEmpty()) {
            scheduledLessonRepository.deleteAll(staleSnapshots);
        }

        scheduledLessonRepository.saveAll(snapshotsToSave);

        scoreEvaluationService.recalculate(schedule.getId());

        log.info("Successfully saved {} snapshot lessons for schedule {}, score={}",
            snapshotsToSave.size(), schedule.getId(), schedule.getSolverScore());
    }

}
