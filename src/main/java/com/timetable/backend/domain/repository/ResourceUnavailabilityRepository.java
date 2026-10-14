package com.timetable.backend.domain.repository;

import com.timetable.backend.domain.model.AvailabilityRole;
import com.timetable.backend.domain.model.ResourceUnavailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ResourceUnavailabilityRepository extends JpaRepository<ResourceUnavailability, Long> {

    // Find all one-time exceptions for a specific user
    List<ResourceUnavailability> findByUserId(Long userId);

    List<ResourceUnavailability> findByUserIdAndAvailabilityRole(Long userId, AvailabilityRole availabilityRole);

    @Modifying
    @Query("DELETE FROM ResourceUnavailability r WHERE r.user.id = :userId AND r.availabilityRole = :role")
    void deleteByUserIdAndAvailabilityRole(@Param("userId") Long userId, @Param("role") AvailabilityRole role);

    // Find exceptions for a specific user on a specific date
    List<ResourceUnavailability> findByUserIdAndDate(Long userId, LocalDate date);

    /**
     * Bulk delete — single DELETE SQL statement instead of N individual removes.
     * Must be called within a @Transactional context.
     */
    @Modifying
    @Query("DELETE FROM ResourceUnavailability r WHERE r.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}