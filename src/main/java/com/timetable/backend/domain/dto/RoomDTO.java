package com.timetable.backend.domain.dto;

public record RoomDTO(
    Long id,
    String name,
    int capacity,
    boolean allowsParallelPrivate
) {}
