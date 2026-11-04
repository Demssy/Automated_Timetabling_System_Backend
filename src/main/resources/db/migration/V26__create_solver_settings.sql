CREATE TABLE solver_settings_preset (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    settings_json TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_solver_settings_preset_name UNIQUE (name)
);

CREATE TABLE solver_settings_state (
    id BIGINT NOT NULL PRIMARY KEY,
    settings_json TEXT NULL,
    active_preset_id BIGINT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_solver_settings_active_preset FOREIGN KEY (active_preset_id)
        REFERENCES solver_settings_preset (id)
);

INSERT INTO solver_settings_state (id, version) VALUES (1, 0);
