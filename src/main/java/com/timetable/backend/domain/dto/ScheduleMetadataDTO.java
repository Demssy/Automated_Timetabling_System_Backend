package com.timetable.backend.domain.dto;

import com.timetable.backend.domain.model.ScheduleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * DTO for ScheduleMetadata.
 */
public record ScheduleMetadataDTO(
    Long id,
    @NotBlank(message = "Name is required")
    String name,
    @NotNull(message = "Valid from date is required")
    LocalDate validFrom,
    @NotNull(message = "Valid to date is required")
    LocalDate validTo,
    String createdAt,
    // Status handling if needed, usually managed by business logic but allowed to be set by admin?
    // Let's assume basic fields for now.
    ScheduleStatus status
) {}

