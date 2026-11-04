package com.timetable.backend.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolverSettingsPresetRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull @Valid SolverSettingsValues settings
) {
}
