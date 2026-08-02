package com.timetable.backend.domain.mapper;

import com.timetable.backend.domain.dto.ScheduleMetadataDTO;
import com.timetable.backend.domain.model.ScheduleMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface ScheduleMetadataMapper {

    /**
     * Maps ScheduleMetadata entity to DTO.
     * Uses an explicit null-safe expression for createdAt instead of {@code dateFormat}
     * to prevent NPE when the DB row has a NULL created_at (e.g. seeded via raw SQL
     * that bypassed the @PrePersist hook).
     */
    @Mapping(target = "createdAt", expression = "java(formatCreatedAt(entity.getCreatedAt()))")
    ScheduleMetadataDTO toDTO(ScheduleMetadata entity);

    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "createdAt",   ignore = true)
    @Mapping(target = "solverScore", ignore = true)
    @Mapping(target = "description", ignore = true)
    ScheduleMetadata toEntity(ScheduleMetadataDTO dto);

    /** Null-safe LocalDateTime → formatted String conversion used by the toDTO expression. */
    default String formatCreatedAt(LocalDateTime dt) {
        return dt == null ? null
                : dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}

