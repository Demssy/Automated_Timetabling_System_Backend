package com.timetable.backend.domain.repository;

import com.timetable.backend.domain.model.SchedulePublicationLock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SchedulePublicationLockRepository extends JpaRepository<SchedulePublicationLock, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from SchedulePublicationLock l where l.id = 1")
    Optional<SchedulePublicationLock> acquire();
}
