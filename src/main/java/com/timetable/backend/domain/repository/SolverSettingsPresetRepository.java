package com.timetable.backend.domain.repository;

import com.timetable.backend.domain.model.SolverSettingsPreset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolverSettingsPresetRepository extends JpaRepository<SolverSettingsPreset, Long> {
    List<SolverSettingsPreset> findAllByOrderByNameAsc();

    boolean existsByName(String name);
}
