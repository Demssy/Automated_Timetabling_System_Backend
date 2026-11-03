package com.timetable.backend.service;

import com.timetable.backend.domain.model.ScheduleMetadata;
import com.timetable.backend.domain.model.ScheduleStatus;
import com.timetable.backend.domain.repository.ScheduleMetadataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolverRunServiceTest {
    @Mock private ScheduleMetadataRepository schedules;

    @Test
    void beginsNewRevisionForDraftOnly() {
        ScheduleMetadata schedule = new ScheduleMetadata();
        schedule.setStatus(ScheduleStatus.DRAFT);
        schedule.setSolverRevision(4);
        when(schedules.findByIdForUpdate(1L)).thenReturn(Optional.of(schedule));
        SolverRunService service = new SolverRunService(schedules);
        assertThat(service.begin(1L)).isEqualTo(5);
        schedule.setStatus(ScheduleStatus.ARCHIVED);
        assertThatThrownBy(() -> service.begin(1L)).isInstanceOf(IllegalStateException.class);
        assertThat(schedule.getSolverRevision()).isEqualTo(5);
    }
}
