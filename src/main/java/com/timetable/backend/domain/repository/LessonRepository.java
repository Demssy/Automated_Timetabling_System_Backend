package com.timetable.backend.domain.repository;

import com.timetable.backend.domain.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

	/**
	 * Loads lessons for a specific schedule id.
	 *
	 * NOTE: Until lessons are explicitly versioned per schedule in the domain model,
	 * this query returns all lessons for non-null schedule ids.
	 */
	@Query("SELECT l FROM Lesson l WHERE :scheduleId IS NOT NULL")
	List<Lesson> findByScheduleId(@Param("scheduleId") Long scheduleId);

	List<Lesson> findByIsActiveTrue();

	@Query("SELECT l FROM Lesson l WHERE :scheduleId IS NOT NULL AND l.isActive = true")
	List<Lesson> findByIsActiveTrueAndScheduleId(@Param("scheduleId") Long scheduleId);

	Lesson findById(long id);

	/**
	 * Finds all active group (non-private) lessons for a given dance group.
	 * Used to build the weekly schedule shown on the Groups page.
	 */
	List<Lesson> findByDanceGroupIdAndIsPrivateFalseAndIsActiveTrue(Long danceGroupId);

	/**
	 * Finds all active group lessons where a given teacher is assigned.
	 * Used for the teacher's "My Groups" view.
	 */
	@Query("SELECT l FROM Lesson l WHERE l.isPrivate = false AND l.isActive = true AND l.teacher.id = :teacherId")
	List<Lesson> findActiveGroupLessonsByTeacherId(@Param("teacherId") Long teacherId);
}

