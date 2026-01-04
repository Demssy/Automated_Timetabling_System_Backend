package com.timetable.backend.domain.repository;

import com.timetable.backend.domain.model.ScheduleMetadata;
import com.timetable.backend.domain.model.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ScheduleMetadata entity.
 * Provides methods for querying schedule versions.
 */
@Repository
public interface ScheduleMetadataRepository extends JpaRepository<ScheduleMetadata, Long> {

    /**
     * Finds schedules by status.
     *
     * @param status the schedule status to filter by
     * @return list of schedules with the given status
     */
    List<ScheduleMetadata> findByStatus(ScheduleStatus status);

    /**
     * Finds the currently published (active) schedule.
     *
     * @return optional containing the published schedule, if exists
     */
    default Optional<ScheduleMetadata> findPublishedSchedule() {
        return findByStatus(ScheduleStatus.PUBLISHED).stream().findFirst();
    }

    /**
     * Finds schedules that are valid (active) on a specific date.
     *
     * @param date the date to check
     * @return list of schedules valid on the given date
     */
    List<ScheduleMetadata> findByValidFromLessThanEqualAndValidToGreaterThanEqual(
        LocalDate date, LocalDate sameDate
    );

    /**
     * Finds schedules ordered by creation date (newest first).
     *
     * @return list of all schedules ordered by creation date descending
     */
    List<ScheduleMetadata> findAllByOrderByCreatedAtDesc();
}

