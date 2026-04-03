package com.timetable.backend.domain.mapper;

import com.timetable.backend.domain.dto.ResourceUnavailabilityDTO;
import com.timetable.backend.domain.model.ResourceUnavailability;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ResourceUnavailabilityMapper {

    ResourceUnavailabilityDTO toDTO(ResourceUnavailability entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true) // Handled in service
    ResourceUnavailability toEntity(ResourceUnavailabilityDTO dto);

    List<ResourceUnavailabilityDTO> toDTOList(List<ResourceUnavailability> entities);

    List<ResourceUnavailability> toEntityList(List<ResourceUnavailabilityDTO> dtos);
}

