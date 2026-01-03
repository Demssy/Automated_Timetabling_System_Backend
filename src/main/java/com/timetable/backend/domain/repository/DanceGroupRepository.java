package com.timetable.backend.domain.repository;

import com.timetable.backend.domain.model.DanceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DanceGroupRepository extends JpaRepository<DanceGroup, Long> {

    Optional<DanceGroup> findByName(String name);
}

