package com.timetable.backend.domain.mapper;

import com.timetable.backend.domain.dto.CreateTeacherRequest;
import com.timetable.backend.domain.dto.TeacherResponse;
import com.timetable.backend.domain.dto.UpdateTeacherRequest;
import com.timetable.backend.domain.model.Teacher;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {DictionaryMapper.class})
public interface TeacherMapper {

    @Mapping(target = "qualifiedStyles", source = "danceStyles")
    TeacherResponse toTeacherResponse(Teacher teacher);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "danceStyles", ignore = true)
    Teacher toTeacher(CreateTeacherRequest request);

    /**
     * Updates an existing Teacher entity from UpdateTeacherRequest DTO.
     * Does not modify id, email, password, or role - these are immutable or managed separately.
     *
     * @param dto the update request with new values
     * @param entity the existing teacher entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "danceStyles", ignore = true)  // Handle separately in service
    void updateTeacherFromDto(UpdateTeacherRequest dto, @MappingTarget Teacher entity);
}
