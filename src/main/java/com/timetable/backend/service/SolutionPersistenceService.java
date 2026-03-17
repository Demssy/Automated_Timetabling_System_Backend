package com.timetable.backend.service;

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
import java.util.HashMap;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
@Slf4j
public class SolutionPersistenceService {

    private final ScheduledLessonRepository scheduledLessonRepository;
    private final ScheduleMetadataRepository scheduleMetadataRepository;

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

            // Skip duplicate lessons from solver output to avoid duplicate rows.
            if (!incomingLessonIds.add(lesson.getId())) {
                log.warn("Duplicate lesson {} detected in solver output for schedule {}. Skipping duplicate row.",
                    lesson.getId(), schedule.getId());
                return;
            }

            var status = lesson.getTimeslot() != null && lesson.getRoom() != null
                ? ScheduledLessonStatus.ASSIGNED
                : ScheduledLessonStatus.UNASSIGNED;

            var existing = existingByLessonId.get(lesson.getId());
            if (existing != null) {
                existing.setTimeslot(lesson.getTimeslot());
                existing.setRoom(lesson.getRoom());
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
                lesson.getRoom()
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
        scheduleMetadataRepository.save(schedule);

        log.info("Successfully saved {} snapshot lessons for schedule {}, score={}",
            snapshotsToSave.size(), schedule.getId(), schedule.getSolverScore());
    }
}
