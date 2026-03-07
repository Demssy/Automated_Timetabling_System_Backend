package com.timetable.backend.domain.dto;

import com.timetable.backend.domain.model.DanceLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for DanceGroup entity.
 */
public record DanceGroupDTO(
    Long id,
    @NotBlank(message = "Name is required")
    String name,
    @NotNull(message = "Dance Style ID is required")
    Long danceStyleId,
    String danceStyleName,
    @NotNull(message = "Dance Level is required")
    DanceLevel danceLevel,
    @Min(value = 1, message = "Min size must be at least 1")
    Integer minSize,
    String targetAgeRange
) {}

