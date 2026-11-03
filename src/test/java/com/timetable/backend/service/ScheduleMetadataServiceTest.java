package com.timetable.backend.service;

import com.timetable.backend.domain.dto.ScheduleMetadataDTO;
import com.timetable.backend.domain.mapper.ScheduleMetadataMapper;
import com.timetable.backend.domain.model.ScheduleMetadata;
import com.timetable.backend.domain.model.SchedulePublicationLock;
import com.timetable.backend.domain.model.ScheduleStatus;
import com.timetable.backend.domain.repository.AddedLessonRepository;
import com.timetable.backend.domain.repository.ScheduleMetadataRepository;
import com.timetable.backend.domain.repository.SchedulePublicationLockRepository;
import com.timetable.backend.domain.repository.ScheduledLessonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleMetadataServiceTest {
    @Mock private ScheduleMetadataRepository repository;
    @Mock private ScheduleMetadataMapper mapper;
    @Mock private AddedLessonRepository addedLessons;
    @Mock private ScheduledLessonRepository snapshots;
    @Mock private ScheduleScoreEvaluationService evaluator;
    @Mock private SchedulePublicationLockRepository publicationLock;
    private ScheduleMetadataService service;
    private ScheduleMetadata schedule;
    private ScheduleMetadataDTO dto;

    @BeforeEach
    void setUp() {
        service = new ScheduleMetadataService(repository, mapper, addedLessons, snapshots, evaluator, publicationLock);
        lenient().when(publicationLock.acquire()).thenReturn(Optional.of(new SchedulePublicationLock()));
        schedule = new ScheduleMetadata();
        schedule.setId(1L);
        schedule.setStatus(ScheduleStatus.DRAFT);
        schedule.setValidFrom(LocalDate.of(2026, 1, 1));
        schedule.setValidTo(LocalDate.of(2026, 1, 7));
        dto = new ScheduleMetadataDTO(1L, "Schedule", schedule.getValidFrom(), schedule.getValidTo(),
                null, ScheduleStatus.PUBLISHED);
    }

    @Test
    void publishRunsGuardBeforeChangingStatus() {
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(schedule));
        doThrow(new IllegalStateException("invalid")).when(evaluator).validateAndRecalculate(1L, true);
        assertThatThrownBy(() -> service.publish(1L)).isInstanceOf(IllegalStateException.class);
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.DRAFT);
        verify(repository, never()).save(any());
    }

    @Test
    void updateCannotBypassPublicationGuard() {
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(schedule));
        doThrow(new IllegalStateException("invalid")).when(evaluator).validateAndRecalculate(1L, true);
        assertThatThrownBy(() -> service.update(1L, dto)).isInstanceOf(IllegalStateException.class);
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.DRAFT);
    }

    @Test
    void createCannotBypassPublicationGuard() {
        schedule.setStatus(ScheduleStatus.PUBLISHED);
        when(mapper.toEntity(dto)).thenReturn(schedule);
        when(repository.save(schedule)).thenReturn(schedule);
        doThrow(new IllegalStateException("invalid")).when(evaluator).validateAndRecalculate(1L, true);
        assertThatThrownBy(() -> service.create(dto)).isInstanceOf(IllegalStateException.class);
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.DRAFT);
    }

    @Test
    void overlappingPublishedPeriodIsRejected() {
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(schedule));
        when(repository.existsByStatusAndIdNotAndValidFromLessThanEqualAndValidToGreaterThanEqual(
                any(), anyLong(), any(), any())).thenReturn(true);
        assertThatThrownBy(() -> service.publish(1L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("same date range");
        verify(evaluator).validateAndRecalculate(1L, true);
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.DRAFT);
    }
}
