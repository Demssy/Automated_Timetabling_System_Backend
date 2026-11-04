package com.timetable.backend.controller;

import com.timetable.backend.domain.dto.SolverSettingsPresetRequest;
import com.timetable.backend.domain.dto.SolverSettingsPresetResponse;
import com.timetable.backend.domain.dto.SolverSettingsResponse;
import com.timetable.backend.domain.dto.SolverSettingsValues;
import com.timetable.backend.service.SolverSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/solver/settings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SolverSettingsController {
    private final SolverSettingsService service;

    @GetMapping
    public SolverSettingsResponse get() {
        return service.get();
    }

    @PutMapping
    public SolverSettingsResponse update(@Valid @RequestBody SolverSettingsValues settings) {
        return service.update(settings);
    }

    @PostMapping("/reset")
    public SolverSettingsResponse reset() {
        return service.reset();
    }

    @GetMapping("/presets")
    public List<SolverSettingsPresetResponse> presets() {
        return service.listPresets();
    }

    @PostMapping("/presets")
    @ResponseStatus(HttpStatus.CREATED)
    public SolverSettingsPresetResponse createPreset(@Valid @RequestBody SolverSettingsPresetRequest request) {
        return service.createPreset(request);
    }

    @PutMapping("/presets/{id}")
    public SolverSettingsPresetResponse updatePreset(@PathVariable Long id,
            @Valid @RequestBody SolverSettingsPresetRequest request) {
        return service.updatePreset(id, request);
    }

    @PostMapping("/presets/{id}/activate")
    public SolverSettingsResponse activatePreset(@PathVariable Long id) {
        return service.activatePreset(id);
    }

    @DeleteMapping("/presets/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePreset(@PathVariable Long id) {
        service.deletePreset(id);
    }
}
