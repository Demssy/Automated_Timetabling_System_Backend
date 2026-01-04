package com.timetable.backend.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record DanceStyleDTO(
    Long id,

    @NotBlank(message = "Dance style name is required")
    String name
) {}
