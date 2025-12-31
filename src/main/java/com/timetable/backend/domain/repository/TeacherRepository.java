package com.timetable.backend.domain.repository;

import com.timetable.backend.domain.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
}

