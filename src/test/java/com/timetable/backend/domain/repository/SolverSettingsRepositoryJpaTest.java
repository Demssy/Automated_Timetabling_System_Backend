package com.timetable.backend.domain.repository;

import com.timetable.backend.domain.model.SolverSettingsPreset;
import com.timetable.backend.domain.model.SolverSettingsState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class SolverSettingsRepositoryJpaTest {
    @Autowired private SolverSettingsStateRepository states;
    @Autowired private SolverSettingsPresetRepository presets;

    @Test
    void locksSingletonAndListsPresetsInNameOrder() {
        SolverSettingsState state = new SolverSettingsState();
        state.setId(1L);
        states.saveAndFlush(state);
        SolverSettingsPreset second = preset("Second");
        SolverSettingsPreset first = preset("First");
        presets.save(second);
        presets.save(first);
        presets.flush();
        SolverSettingsState locked = states.findForUpdate().orElseThrow();
        locked.setActivePresetId(first.getId());
        states.saveAndFlush(locked);
        assertThat(states.findById(1L).orElseThrow().getActivePresetId()).isEqualTo(first.getId());
        assertThat(presets.findAllByOrderByNameAsc()).extracting(SolverSettingsPreset::getName)
                .containsExactly("First", "Second");
        assertThat(presets.existsByName("First")).isTrue();
    }

    private SolverSettingsPreset preset(String name) {
        SolverSettingsPreset preset = new SolverSettingsPreset();
        preset.setName(name);
        preset.setSettingsJson("{}");
        return preset;
    }
}
