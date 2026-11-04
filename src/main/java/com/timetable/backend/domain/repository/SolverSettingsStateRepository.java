package com.timetable.backend.domain.repository;

import com.timetable.backend.domain.model.SolverSettingsState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SolverSettingsStateRepository extends JpaRepository<SolverSettingsState, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SolverSettingsState s where s.id = 1")
    Optional<SolverSettingsState> findForUpdate();
}
