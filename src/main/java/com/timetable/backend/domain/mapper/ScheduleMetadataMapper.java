package com.timetable.backend.domain.mapper;

import com.timetable.backend.domain.dto.ScheduleMetadataDTO;
import com.timetable.backend.domain.model.ScheduleMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ScheduleMetadataMapper {

    @Mapping(target = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    ScheduleMetadataDTO toDTO(ScheduleMetadata entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "solverScore", ignore = true)
    @Mapping(target = "description", ignore = true) // Add to DTO if needed
    ScheduleMetadata toEntity(ScheduleMetadataDTO dto);
}

