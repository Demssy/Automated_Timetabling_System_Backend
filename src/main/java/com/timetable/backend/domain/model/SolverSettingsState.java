package com.timetable.backend.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "solver_settings_state")
@Getter
@Setter
public class SolverSettingsState {
    @Id
    private Long id;

    @Column(name = "settings_json", columnDefinition = "TEXT")
    private String settingsJson;

    @Column(name = "active_preset_id")
    private Long activePresetId;

    @Version
    private long version;
}
