package com.timetable.backend.domain.repository;

import com.timetable.backend.domain.model.WeeklyAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeeklyAvailabilityRepository extends JpaRepository<WeeklyAvailability, Long> {

    // Find weekly schedule for a specific user
    List<WeeklyAvailability> findByUserId(Long userId);

    /**
     * Bulk delete — single DELETE SQL statement instead of N individual removes.
     * Must be called within a @Transactional context.
     */
    @Modifying
    @Query("DELETE FROM WeeklyAvailability w WHERE w.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}