package com.timetable.backend.domain.repository;

import com.timetable.backend.domain.model.DanceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DanceGroupRepository extends JpaRepository<DanceGroup, Long> {

    Optional<DanceGroup> findByName(String name);

    /**
     * Finds all groups where the student with the given id is enrolled.
     */
    @Query("SELECT g FROM DanceGroup g JOIN g.enrolledStudents s WHERE s.id = :studentId")
    List<DanceGroup> findByEnrolledStudentId(@Param("studentId") Long studentId);
}

