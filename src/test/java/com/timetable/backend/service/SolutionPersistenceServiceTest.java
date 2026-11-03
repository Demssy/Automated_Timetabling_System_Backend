package com.timetable.backend.service;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import com.timetable.backend.domain.model.Lesson;
import com.timetable.backend.domain.model.ScheduleMetadata;
import com.timetable.backend.domain.model.ScheduledLesson;
import com.timetable.backend.domain.model.ScheduleStatus;
import com.timetable.backend.domain.repository.ScheduleMetadataRepository;
import com.timetable.backend.domain.repository.ScheduledLessonRepository;
import com.timetable.backend.solver.DanceSchedule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolutionPersistenceServiceTest {

    @Mock
    private ScheduledLessonRepository scheduledLessonRepository;
    @Mock
    private ScheduleMetadataRepository scheduleMetadataRepository;
    @Mock
    private ScheduleScoreEvaluationService scoreEvaluationService;

    private SolutionPersistenceService service;
    private ScheduleMetadata schedule;
    private ScheduledLesson cancelled;

    @BeforeEach
    void setUp() {
        service = new SolutionPersistenceService(scheduledLessonRepository, scheduleMetadataRepository,
                scoreEvaluationService);
        schedule = new ScheduleMetadata();
        schedule.setId(1L);
        schedule.setStatus(ScheduleStatus.DRAFT);
        Lesson source = new Lesson();
        source.setId(10L);
        source.setPrivate(true);
        cancelled = new ScheduledLesson();
        cancelled.setLesson(source);
        cancelled.setSchedule(schedule);
        cancelled.setCancelled(true);
        when(scheduleMetadataRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(schedule));
        lenient().when(scheduledLessonRepository.findByScheduleIdOrderByIdAsc(1L)).thenReturn(List.of(cancelled));
    }

    @Test
    void retainsCancelledPrivateSnapshotWhenSolverLeavesStudentUnassigned() {
        Lesson unmatched = new Lesson();
        unmatched.setId(10L);
        unmatched.setPrivate(true);
        service.saveSolution(solution(List.of(unmatched)), 0);
        verify(scheduledLessonRepository, never()).deleteAll(any());
        verify(scheduledLessonRepository).saveAll(List.of());
        verify(scoreEvaluationService).recalculate(1L);
    }

    @Test
    void retainsCancelledSnapshotMissingFromSolverOutput() {
        service.saveSolution(solution(List.of()), 0);
        verify(scheduledLessonRepository, never()).deleteAll(any());
        verify(scheduledLessonRepository).saveAll(List.of());
        verify(scoreEvaluationService).recalculate(1L);
    }

    @Test
    void ignoresStaleResultEvenIfScheduleIsDraftAgain() {
        schedule.setSolverRevision(3);
        service.saveSolution(solution(List.of()), 1);
        verify(scheduledLessonRepository, never()).findByScheduleIdOrderByIdAsc(any());
        verify(scoreEvaluationService, never()).recalculate(any());
    }

    @Test
    void ignoresResultForPublishedSchedule() {
        schedule.setStatus(ScheduleStatus.PUBLISHED);
        service.saveSolution(solution(List.of()), 0);
        verify(scheduledLessonRepository, never()).findByScheduleIdOrderByIdAsc(any());
        verify(scoreEvaluationService, never()).recalculate(any());
    }

    private DanceSchedule solution(List<Lesson> lessons) {
        DanceSchedule solution = new DanceSchedule(1L, List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), lessons);
        solution.setScore(HardSoftScore.ZERO);
        return solution;
    }
}
