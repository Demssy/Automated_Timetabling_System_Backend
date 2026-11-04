package com.timetable.backend.domain.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record SolverSettingsResponse(
        @JsonUnwrapped SolverSettingsValues settings,
        SolverSettingsValues defaults,
        Long activePresetId
) {
}
