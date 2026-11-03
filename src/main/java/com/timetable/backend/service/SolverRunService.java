package com.timetable.backend.service;

import com.timetable.backend.domain.model.ScheduleMetadata;
import com.timetable.backend.domain.model.ScheduleStatus;
import com.timetable.backend.domain.exception.ScheduleConflictException;
import com.timetable.backend.domain.repository.ScheduleMetadataRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SolverRunService {
    private final ScheduleMetadataRepository scheduleRepository;

    @Transactional
    public long begin(Long scheduleId) {
        ScheduleMetadata schedule = scheduleRepository.findByIdForUpdate(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found: " + scheduleId));
        if (schedule.getStatus() != ScheduleStatus.DRAFT) {
            throw new ScheduleConflictException("Solver can only run for a draft schedule");
        }
        schedule.setSolverRevision(Math.incrementExact(schedule.getSolverRevision()));
        return schedule.getSolverRevision();
    }
}
