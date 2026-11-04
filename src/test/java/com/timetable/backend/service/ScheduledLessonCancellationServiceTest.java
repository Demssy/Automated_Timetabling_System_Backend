package com.timetable.backend.service;

import com.timetable.backend.domain.exception.ScheduleConflictException;
import com.timetable.backend.domain.mapper.LessonMapper;
import com.timetable.backend.domain.model.AbstractUser;
import com.timetable.backend.domain.model.ScheduleMetadata;
import com.timetable.backend.domain.model.ScheduleStatus;
import com.timetable.backend.domain.model.ScheduledLesson;
import com.timetable.backend.domain.repository.ScheduleMetadataRepository;
import com.timetable.backend.domain.repository.ScheduledLessonRepository;
import com.timetable.backend.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledLessonCancellationServiceTest {
    @Mock private ScheduledLessonRepository snapshots;
    @Mock private UserRepository users;
    @Mock private LessonMapper mapper;
    @Mock private ScheduleScoreEvaluationService evaluator;
    @Mock private ScheduleMetadataRepository schedules;
    private ScheduledLessonCancellationService service;
    private ScheduleMetadata schedule;
    private ScheduledLesson lesson;
    private Authentication admin;

    @BeforeEach
    void setUp() {
        service = new ScheduledLessonCancellationService(snapshots, users, mapper, evaluator, schedules);
        schedule = new ScheduleMetadata();
        schedule.setId(1L);
        schedule.setStatus(ScheduleStatus.PUBLISHED);
        lesson = new ScheduledLesson();
        lesson.setId(2L);
        lesson.setSchedule(schedule);
        admin = new UsernamePasswordAuthenticationToken("admin@example.test", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(schedules.findBySnapshotIdForUpdate(2L)).thenReturn(Optional.of(schedule));
        when(snapshots.findById(2L)).thenReturn(Optional.of(lesson));
        when(users.findByEmail("admin@example.test")).thenReturn(Optional.of(new AbstractUser()));
        when(snapshots.save(lesson)).thenReturn(lesson);
    }

    @Test
    void publishedCancellationPropagatesScoreGuardFailure() {
        doThrow(new ScheduleConflictException("hard score below threshold"))
                .when(evaluator).validateAndRecalculate(1L, false);
        assertThatThrownBy(() -> service.cancelLesson(2L, null, admin))
                .isInstanceOf(ScheduleConflictException.class);
        verify(evaluator).validateAndRecalculate(1L, false);
    }

    @Test
    void draftCancellationRecalculatesWithoutPublicationGuard() {
        schedule.setStatus(ScheduleStatus.DRAFT);
        service.cancelLesson(2L, null, admin);
        verify(evaluator).recalculate(1L);
    }
}
