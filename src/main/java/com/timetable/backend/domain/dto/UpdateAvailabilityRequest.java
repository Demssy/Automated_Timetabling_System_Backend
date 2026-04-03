package com.timetable.backend.domain.dto;

import java.util.List;

public record UpdateAvailabilityRequest(
        List<WeeklyAvailabilityDTO> weeklyAvailabilities,
        List<ResourceUnavailabilityDTO> oneTimeUnavailabilities
) {}