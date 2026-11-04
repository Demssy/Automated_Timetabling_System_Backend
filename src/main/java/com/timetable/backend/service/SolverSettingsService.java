package com.timetable.backend.service;

import com.timetable.backend.config.SolverWeightsConfig;
import com.timetable.backend.domain.dto.SolverSettingsPresetRequest;
import com.timetable.backend.domain.dto.SolverSettingsPresetResponse;
import com.timetable.backend.domain.dto.SolverSettingsResponse;
import com.timetable.backend.domain.dto.SolverSettingsValues;
import com.timetable.backend.domain.mapper.SolverSettingsMapper;
import com.timetable.backend.domain.model.SolverSettingsPreset;
import com.timetable.backend.domain.model.SolverSettingsState;
import com.timetable.backend.domain.repository.SolverSettingsPresetRepository;
import com.timetable.backend.domain.repository.SolverSettingsStateRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SolverSettingsService implements ApplicationRunner {
    private final SolverSettingsStateRepository stateRepository;
    private final SolverSettingsPresetRepository presetRepository;
    private final SolverSettingsMapper mapper;
    private final SolverSettingsValues defaults;

    public SolverSettingsService(SolverSettingsStateRepository stateRepository,
            SolverSettingsPresetRepository presetRepository, SolverSettingsMapper mapper,
            SolverWeightsConfig weights,
            @Value("${timefold.solver.termination.spent-limit:10s}") Duration spentLimit,
            Validator validator) {
        this.stateRepository = stateRepository;
        this.presetRepository = presetRepository;
        this.mapper = mapper;
        this.defaults = new SolverSettingsValues(
                Math.toIntExact(spentLimit.toSeconds()), 0, weights.getMaxTeacherLessonsPerDay(),
                Math.toIntExact(weights.getTeacherGapThresholdMinutes()), 1, 1, 1, 1, 1,
                weights.getRewardStudentAssignment(), weights.getRewardPrimeTime(),
                weights.getPenaltyStudentUnderDesiredLessons(), weights.getPenaltyTeacherUnderDesiredLessons(), 1, 1);
        var violations = validator.validate(defaults);
        if (!violations.isEmpty()) {
            String errors = violations.stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw new IllegalStateException("Invalid configured solver defaults: " + errors);
        }
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!stateRepository.existsById(1L)) {
            SolverSettingsState state = new SolverSettingsState();
            state.setId(1L);
            stateRepository.save(state);
        }
    }

    @Transactional(readOnly = true)
    public SolverSettingsValues current() {
        return values(state());
    }

    @Transactional(readOnly = true)
    public SolverSettingsResponse get() {
        return response(state());
    }

    @Transactional
    public SolverSettingsResponse update(SolverSettingsValues settings) {
        SolverSettingsState state = lockedState();
        state.setSettingsJson(mapper.serialize(settings));
        state.setActivePresetId(null);
        return response(state);
    }

    @Transactional
    public SolverSettingsResponse reset() {
        SolverSettingsState state = lockedState();
        state.setSettingsJson(null);
        state.setActivePresetId(null);
        return response(state);
    }

    @Transactional(readOnly = true)
    public List<SolverSettingsPresetResponse> listPresets() {
        return presetRepository.findAllByOrderByNameAsc().stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public SolverSettingsPresetResponse createPreset(SolverSettingsPresetRequest request) {
        lockedState();
        if (presetRepository.existsByName(request.name().trim())) {
            throw new IllegalArgumentException("A solver settings preset with this name already exists");
        }
        SolverSettingsPreset preset = new SolverSettingsPreset();
        preset.setName(request.name().trim());
        preset.setSettingsJson(mapper.serialize(request.settings()));
        return mapper.toResponse(presetRepository.save(preset));
    }

    @Transactional
    public SolverSettingsPresetResponse updatePreset(Long id, SolverSettingsPresetRequest request) {
        SolverSettingsState state = lockedState();
        SolverSettingsPreset preset = preset(id);
        String name = request.name().trim();
        if (!preset.getName().equals(name) && presetRepository.existsByName(name)) {
            throw new IllegalArgumentException("A solver settings preset with this name already exists");
        }
        preset.setName(name);
        preset.setSettingsJson(mapper.serialize(request.settings()));
        if (id.equals(state.getActivePresetId())) {
            state.setSettingsJson(preset.getSettingsJson());
        }
        return mapper.toResponse(preset);
    }

    @Transactional
    public SolverSettingsResponse activatePreset(Long id) {
        SolverSettingsState state = lockedState();
        SolverSettingsPreset preset = preset(id);
        state.setActivePresetId(id);
        state.setSettingsJson(preset.getSettingsJson());
        return response(state);
    }

    @Transactional
    public void deletePreset(Long id) {
        SolverSettingsState state = lockedState();
        SolverSettingsPreset preset = preset(id);
        if (id.equals(state.getActivePresetId())) {
            state.setActivePresetId(null);
            stateRepository.saveAndFlush(state);
        }
        presetRepository.delete(preset);
    }

    private SolverSettingsPreset preset(Long id) {
        return presetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solver settings preset not found: " + id));
    }

    private SolverSettingsState state() {
        return stateRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("Solver settings state is missing"));
    }

    private SolverSettingsState lockedState() {
        return stateRepository.findForUpdate()
                .orElseThrow(() -> new IllegalStateException("Solver settings state is missing"));
    }

    private SolverSettingsValues values(SolverSettingsState state) {
        return state.getSettingsJson() == null ? defaults : mapper.deserialize(state.getSettingsJson());
    }

    private SolverSettingsResponse response(SolverSettingsState state) {
        return new SolverSettingsResponse(values(state), defaults, state.getActivePresetId());
    }
}
