package com.timetable.backend.domain.mapper;

import com.timetable.backend.domain.dto.ResourceUnavailabilityDTO;
import com.timetable.backend.domain.model.ResourceUnavailability;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ResourceUnavailabilityMapper {

    @Mapping(source = "teacher.id", target = "teacherId")
    @Mapping(source = "teacher.user.fullName", target = "teacherName")
    @Mapping(source = "timeslot.id", target = "timeslotId")
    @Mapping(expression = "java(entity.getTimeslot().getDayOfWeek() + \" \" + entity.getTimeslot().getStartTime() + \"-\" + entity.getTimeslot().getEndTime())", target = "timeslotDescription")
    ResourceUnavailabilityDTO toDTO(ResourceUnavailability entity);

    @Mapping(target = "teacher", ignore = true) // Handled in service
    @Mapping(target = "timeslot", ignore = true) // Handled in service
    ResourceUnavailability toEntity(ResourceUnavailabilityDTO dto);
}

