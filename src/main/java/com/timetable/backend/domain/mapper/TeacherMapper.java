package com.timetable.backend.domain.mapper;

import com.timetable.backend.domain.dto.TeacherResponse;
import com.timetable.backend.domain.dto.UpdateTeacherRequest;
import com.timetable.backend.domain.model.Teacher;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {DictionaryMapper.class})
public interface TeacherMapper {

    @Mapping(target = "qualifiedStyles", source = "danceStyles")
    @Mapping(target = "email",    source = "user.email")
    @Mapping(target = "fullName", source = "user.fullName")
    TeacherResponse toTeacherResponse(Teacher teacher);

    /**
     * Updates teacher-specific fields only.
     * user, id, and danceStyles are handled separately in the service.
     */
    @Mapping(target = "id",             ignore = true)
    @Mapping(target = "user",           ignore = true)
    @Mapping(target = "danceStyles",    ignore = true)
    @Mapping(target = "privateStudents", ignore = true)
    void updateTeacherFromDto(UpdateTeacherRequest dto, @MappingTarget Teacher entity);
}
