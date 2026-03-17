package com.timetable.backend.domain.repository;

import com.timetable.backend.domain.model.ScheduledLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduledLessonRepository extends JpaRepository<ScheduledLesson, Long> {
    List<ScheduledLesson> findByScheduleIdOrderByIdAsc(Long scheduleId);

    void deleteByScheduleId(Long scheduleId);
}
