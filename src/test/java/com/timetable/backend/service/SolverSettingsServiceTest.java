package com.timetable.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timetable.backend.config.SolverWeightsConfig;
import com.timetable.backend.domain.dto.SolverSettingsPresetRequest;
import com.timetable.backend.domain.dto.SolverSettingsValues;
import com.timetable.backend.domain.mapper.SolverSettingsMapper;
import com.timetable.backend.domain.model.SolverSettingsPreset;
import com.timetable.backend.domain.model.SolverSettingsState;
import com.timetable.backend.domain.repository.SolverSettingsPresetRepository;
import com.timetable.backend.domain.repository.SolverSettingsStateRepository;
import jakarta.validation.Validator;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolverSettingsServiceTest {
    @Mock private SolverSettingsStateRepository states;
    @Mock private SolverSettingsPresetRepository presets;
    @Mock private Validator validator;
    private SolverSettingsService service;
    private SolverSettingsState state;
    private SolverSettingsMapper mapper;

    @BeforeEach
    void setUp() {
        SolverWeightsConfig defaults = new SolverWeightsConfig();
        defaults.setRewardPrimeTime(10);
        mapper = new SolverSettingsMapper(new ObjectMapper());
        service = new SolverSettingsService(states, presets, mapper, defaults, Duration.ofSeconds(10), validator);
        state = new SolverSettingsState();
        state.setId(1L);
    }

    @Test
    void updateAndResetReplaceActiveValuesWithoutChangingDefaults() {
        when(states.findForUpdate()).thenReturn(Optional.of(state));
        SolverSettingsValues changed = values(10, 5);
        assertThat(service.update(changed).settings()).isEqualTo(changed);
        assertThat(service.reset().settings()).isEqualTo(values(1, 0));
        assertThat(state.getActivePresetId()).isNull();
        assertThat(state.getSettingsJson()).isNull();
    }

    @Test
    void activatingPresetCopiesItsValuesToActiveState() {
        SolverSettingsPreset preset = new SolverSettingsPreset();
        preset.setId(7L);
        preset.setSettingsJson(mapper.serialize(values(10, 5)));
        when(states.findForUpdate()).thenReturn(Optional.of(state));
        when(presets.findById(7L)).thenReturn(Optional.of(preset));
        assertThat(service.activatePreset(7L).settings()).isEqualTo(values(10, 5));
        assertThat(state.getActivePresetId()).isEqualTo(7L);
    }

    @Test
    void duplicatePresetNamesAreRejected() {
        when(states.findForUpdate()).thenReturn(Optional.of(state));
        when(presets.existsByName("Competition")).thenReturn(true);
        assertThatThrownBy(() -> service.createPreset(new SolverSettingsPresetRequest("Competition", values(1, 0))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("already exists");
    }

    @Test
    void configuredDefaultsMustRespectSoftWeightLimit() {
        SolverWeightsConfig defaults = new SolverWeightsConfig();
        defaults.setRewardStudentAssignment(101);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThatThrownBy(() -> new SolverSettingsService(states, presets, mapper, defaults,
                    Duration.ofSeconds(10), factory.getValidator()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("privateLessonAssignmentReward");
        }
    }

    private SolverSettingsValues values(int physical, int threshold) {
        return new SolverSettingsValues(10, threshold, 6, 15, physical, 1, 1, 1, 1, 100, 10, 50, 30, 1, 1);
    }
}
