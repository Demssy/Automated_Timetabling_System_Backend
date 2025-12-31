package com.timetable.backend.domain.mapper;

import com.timetable.backend.domain.dto.CreateTeacherRequest;
import com.timetable.backend.domain.dto.TeacherResponse;
import com.timetable.backend.domain.model.Teacher;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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
}
