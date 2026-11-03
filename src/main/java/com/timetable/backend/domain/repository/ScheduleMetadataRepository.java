package com.timetable.backend.domain.repository;

import com.timetable.backend.domain.model.ScheduleMetadata;
import com.timetable.backend.domain.model.ScheduleStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleMetadataRepository extends JpaRepository<ScheduleMetadata, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ScheduleMetadata s where s.id = :id")
    Optional<ScheduleMetadata> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ScheduleMetadata s where s.id = "
            + "(select sl.schedule.id from ScheduledLesson sl where sl.id = :snapshotId)")
    Optional<ScheduleMetadata> findBySnapshotIdForUpdate(@Param("snapshotId") Long snapshotId);

    /**
     * Returns all schedules that match the given status.
     * Used to efficiently fetch PUBLISHED schedules at the DB level.
     */
    List<ScheduleMetadata> findAllByStatus(ScheduleStatus status);

    /**
     * Checks whether a PUBLISHED schedule (other than the one being published) overlaps
     * with the given date range.
     *
     * Overlap condition: existingValidFrom <= newValidTo  AND  existingValidTo >= newValidFrom
     *
     * @param status       the status to filter by (PUBLISHED)
     * @param excludeId    the ID of the schedule being published (excluded from the check)
     * @param newValidTo   upper bound of the new schedule's validity period
     * @param newValidFrom lower bound of the new schedule's validity period
     */
    boolean existsByStatusAndIdNotAndValidFromLessThanEqualAndValidToGreaterThanEqual(
            ScheduleStatus status,
            Long excludeId,
            LocalDate newValidTo,
            LocalDate newValidFrom
    );
}
