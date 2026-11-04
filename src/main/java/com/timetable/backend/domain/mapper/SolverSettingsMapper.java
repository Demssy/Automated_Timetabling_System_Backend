package com.timetable.backend.domain.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timetable.backend.domain.dto.SolverSettingsPresetResponse;
import com.timetable.backend.domain.dto.SolverSettingsValues;
import com.timetable.backend.domain.model.SolverSettingsPreset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SolverSettingsMapper {
    private final ObjectMapper objectMapper;

    public String serialize(SolverSettingsValues values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize solver settings", e);
        }
    }

    public SolverSettingsValues deserialize(String json) {
        try {
            return objectMapper.readValue(json, SolverSettingsValues.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot deserialize solver settings", e);
        }
    }

    public SolverSettingsPresetResponse toResponse(SolverSettingsPreset preset) {
        return new SolverSettingsPresetResponse(preset.getId(), preset.getName(), deserialize(preset.getSettingsJson()));
    }
}
