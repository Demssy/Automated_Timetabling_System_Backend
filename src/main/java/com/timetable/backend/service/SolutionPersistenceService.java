package com.timetable.backend.service;

import com.timetable.backend.domain.model.Lesson;
import com.timetable.backend.domain.repository.LessonRepository;
import com.timetable.backend.solver.DanceSchedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SolutionPersistenceService {

    private final LessonRepository lessonRepository;

    @Transactional
    public void saveSolution(DanceSchedule solution) {
        log.info("SolutionPersistenceService: Saving solution for schedule ID: {}, score: {}",
                solution.getId(), solution.getScore());

        if (solution.getScore() == null) {
            log.warn("Solution score is null, skipping save");
            return;
        }

        solution.getLessonList().forEach(lesson -> {
            log.info("Saving lesson {}: timeslot={}, room={}",
                    lesson.getId(),
                    lesson.getTimeslot() != null ? lesson.getTimeslot().getId() : "null",
                    lesson.getRoom() != null ? lesson.getRoom().getId() : "null");

            Lesson persistedLesson = lessonRepository.findById(lesson.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + lesson.getId()));

            persistedLesson.setTimeslot(lesson.getTimeslot());
            persistedLesson.setRoom(lesson.getRoom());

            lessonRepository.save(persistedLesson);
        });

        lessonRepository.flush();
        log.info("Successfully saved solution with {} lessons", solution.getLessonList().size());
    }
}
