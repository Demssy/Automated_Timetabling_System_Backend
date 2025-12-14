package com.timetable.backend.domain.repository;

import com.timetable.backend.domain.model.DanceStyle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DanceStyleRepository extends JpaRepository<DanceStyle, Long> {
    Optional<DanceStyle> findByName(String name);
}

