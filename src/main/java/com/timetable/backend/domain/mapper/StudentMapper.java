package com.timetable.backend.domain.mapper;

import com.timetable.backend.domain.dto.StudentDTO;
import com.timetable.backend.domain.dto.StudentResponse;
import com.timetable.backend.domain.model.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting Student entities to DTOs.
 * Ensures sensitive data (password, system fields) is not exposed.
 */
@Mapper(componentModel = "spring")
public interface StudentMapper {

    /**
     * Maps a Student entity to StudentResponse DTO.
     * Excludes sensitive fields like passwordHash and internal system fields.
     * All fields are mapped directly as Student inherits from AbstractUser.
     *
     * @param student the student entity
     * @return student response DTO with safe public data
     */
    StudentResponse toStudentResponse(Student student);

    @Mapping(target = "password", ignore = true)
    StudentDTO toStudentDTO(Student student);

    @Mapping(target = "passwordHash",      ignore = true)
    @Mapping(target = "role",              ignore = true)
    @Mapping(target = "active",            ignore = true)
    @Mapping(target = "preferredTeachers", ignore = true)
    Student toStudent(StudentDTO studentDTO);
}
