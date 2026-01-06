package com.timetable.backend.domain.repository;

import com.timetable.backend.domain.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Lesson entity.
 *
 * IMPORTANT: When loading lessons for Timefold Solver, always use findAllForSolver()
 * to avoid LazyInitializationException during Planning Model mapping.
 */
@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    /**
     * Loads all lessons with EAGER fetching of relations.
     * This method should be used when loading data for Timefold Solver
     * to ensure all required data is loaded before converting to Planning POJOs.
     *
     * Eager-loaded relations:
     * - teacher (required)
     * - danceGroup and its danceStyle (required)
     * - timeslot (optional, may be null for unscheduled lessons)
     * - room (optional, may be null for unscheduled lessons)
     *
     * @return list of lessons with all relations eagerly loaded
     */
    @Query("""
        SELECT DISTINCT l FROM Lesson l
        JOIN FETCH l.teacher
        JOIN FETCH l.danceGroup dg
        LEFT JOIN FETCH dg.danceStyle
        LEFT JOIN FETCH l.timeslot
        LEFT JOIN FETCH l.room
        """)
    List<Lesson> findAllForSolver();

    /**
     * Alternative: Load all lessons with relations for a specific range.
     * Useful for pagination in large datasets.
     */
    @Query("""
        SELECT DISTINCT l FROM Lesson l
        JOIN FETCH l.teacher
        JOIN FETCH l.danceGroup dg
        LEFT JOIN FETCH dg.danceStyle
        LEFT JOIN FETCH l.timeslot
        LEFT JOIN FETCH l.room
        WHERE l.id >= :startId AND l.id <= :endId
        """)
    List<Lesson> findAllForSolverInRange(Long startId, Long endId);
}

