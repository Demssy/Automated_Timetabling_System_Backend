package com.timetable.backend.domain.repository;

import com.timetable.backend.domain.model.ScheduledLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduledLessonRepository extends JpaRepository<ScheduledLesson, Long> {
    List<ScheduledLesson> findByScheduleIdOrderByIdAsc(Long scheduleId);

    void deleteByScheduleId(Long scheduleId);

    /**
     * Returns (studentId, lessonCount) pairs for all students who received
     * at least one private lesson in the given schedule snapshot.
     */
    @Query("""
        SELECT sl.student.id, COUNT(sl)
        FROM ScheduledLesson sl
        WHERE sl.schedule.id = :scheduleId
          AND sl.student IS NOT NULL
        GROUP BY sl.student.id
        """)
    List<Object[]> countAssignedLessonsByStudent(@Param("scheduleId") Long scheduleId);

    /**
     * Returns all scheduled lessons for a given teacher within a specific schedule snapshot.
     * Sorting is handled in the service layer.
     */
    @Query("""
        SELECT sl FROM ScheduledLesson sl
        WHERE sl.schedule.id = :scheduleId
          AND sl.lesson.teacher.id = :teacherId
        """)
    List<ScheduledLesson> findByScheduleIdAndTeacherId(
        @Param("scheduleId") Long scheduleId,
        @Param("teacherId") Long teacherId
    );

    /**
     * Returns all scheduled lessons for a given student within a specific schedule snapshot.
     * Covers private lessons (solver-assigned) and group lessons (enrolled in dance group).
     *
     * ORDER BY intentionally omitted: MySQL rejects ordering by non-SELECT columns with DISTINCT.
     * Sorting is handled in the service layer instead.
     */
    @Query("""
        SELECT DISTINCT sl FROM ScheduledLesson sl
        LEFT JOIN sl.lesson.danceGroup.enrolledStudents es
        WHERE sl.schedule.id = :scheduleId
          AND (
            sl.student.id = :studentId
            OR (sl.lesson.isPrivate = false AND es.id = :studentId)
          )
        """)
    List<ScheduledLesson> findByScheduleIdAndStudentId(
        @Param("scheduleId") Long scheduleId,
        @Param("studentId") Long studentId
    );
}
