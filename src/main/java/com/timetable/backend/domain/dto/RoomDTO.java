package com.timetable.backend.domain.dto;

import jakarta.validation.constraints.*;

public record RoomDTO(
    Long id,

    @NotBlank(message = "Room name is required")
    String name,

    @Min(value = 1, message = "Capacity must be at least 1")
    int capacity,

    boolean allowsParallelPrivate
) {}
