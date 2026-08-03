package com.timetable.backend.domain.repository;

import com.timetable.backend.domain.model.AddedLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddedLessonRepository extends JpaRepository<AddedLesson, Long> {
    List<AddedLesson> findByScheduleIdOrderByIdAsc(Long scheduleId);
}

