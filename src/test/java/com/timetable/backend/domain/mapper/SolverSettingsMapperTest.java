package com.timetable.backend.domain.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timetable.backend.domain.dto.SolverSettingsValues;
import com.timetable.backend.domain.model.SolverSettingsPreset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SolverSettingsMapperTest {
    private final SolverSettingsMapper mapper = new SolverSettingsMapper(new ObjectMapper());

    @Test
    void mapsPresetAndRoundTripsValues() {
        SolverSettingsValues values = new SolverSettingsValues(
                10, 5, 6, 15, 10, 1, 1, 0, 1, 100, 10, 50, 30, 1, 1);
        SolverSettingsPreset preset = new SolverSettingsPreset();
        preset.setId(7L);
        preset.setName("Competition");
        preset.setSettingsJson(mapper.serialize(values));
        assertThat(mapper.toResponse(preset).settings()).isEqualTo(values);
        assertThat(mapper.toResponse(preset).name()).isEqualTo("Competition");
    }

    @Test
    void corruptStoredSettingsFailExplicitly() {
        assertThatThrownBy(() -> mapper.deserialize("{"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("deserialize");
    }
}
