package com.timetable.backend.domain.repository;

import com.timetable.backend.domain.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    /**
     * Checks if a student belongs to the given teacher's private lesson pool.
     * Uses a COUNT query to avoid loading collections into memory.
     */
    @Query("SELECT COUNT(s) > 0 FROM Teacher t JOIN t.privateStudents s WHERE t.id = :teacherId AND s.id = :studentId")
    boolean isStudentOfTeacher(@Param("teacherId") Long teacherId, @Param("studentId") Long studentId);
}
