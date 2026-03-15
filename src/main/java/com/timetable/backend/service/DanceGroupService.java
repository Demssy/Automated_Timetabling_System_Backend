package com.timetable.backend.service;

import com.timetable.backend.domain.dto.DanceGroupDTO;
import com.timetable.backend.domain.mapper.DictionaryMapper;
import com.timetable.backend.domain.model.DanceGroup;
import com.timetable.backend.domain.model.DanceStyle;
import com.timetable.backend.domain.repository.DanceGroupRepository;
import com.timetable.backend.domain.repository.DanceStyleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing DanceGroup entities.
 */
@Service
@RequiredArgsConstructor
public class DanceGroupService {

    private final DanceGroupRepository danceGroupRepository;
    private final DanceStyleRepository danceStyleRepository;
    private final DictionaryMapper dictionaryMapper;

    @Transactional(readOnly = true)
    public List<DanceGroupDTO> getAllGroups() {
        return danceGroupRepository.findAll().stream()
            .map(dictionaryMapper::toDanceGroupDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public DanceGroupDTO getGroupById(Long id) {
        DanceGroup group = danceGroupRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("DanceGroup not found with id: " + id));
        return dictionaryMapper.toDanceGroupDTO(group);
    }

    @Transactional
    public DanceGroupDTO createGroup(DanceGroupDTO dto) {
        DanceGroup group = dictionaryMapper.toDanceGroup(dto);
        group.setDanceStyle(resolveDanceStyle(dto.danceStyleId()));

        DanceGroup saved = danceGroupRepository.save(group);
        return dictionaryMapper.toDanceGroupDTO(saved);
    }

    @Transactional
    public DanceGroupDTO updateGroup(Long id, DanceGroupDTO dto) {
        DanceGroup group = danceGroupRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("DanceGroup not found with id: " + id));

        group.setName(dto.name());
        group.setDanceLevel(dto.danceLevel());
        group.setMinSize(dto.minSize());
        group.setTargetAgeRange(dto.targetAgeRange());

        if (dto.danceStyleId() != null) {
            Long currentStyleId = group.getDanceStyle() != null ? group.getDanceStyle().getId() : null;
            if (!dto.danceStyleId().equals(currentStyleId)) {
                group.setDanceStyle(resolveDanceStyle(dto.danceStyleId()));
            }
        }

        DanceGroup saved = danceGroupRepository.save(group);
        return dictionaryMapper.toDanceGroupDTO(saved);
    }

    @Transactional
    public void deleteGroup(Long id) {
        if (!danceGroupRepository.existsById(id)) {
            throw new IllegalArgumentException("DanceGroup not found with id: " + id);
        }
        danceGroupRepository.deleteById(id);
    }

    private DanceStyle resolveDanceStyle(Long danceStyleId) {
        if (danceStyleId == null) {
            throw new IllegalArgumentException("Dance Style ID is required");
        }
        return danceStyleRepository.findById(danceStyleId)
            .orElseThrow(() -> new IllegalArgumentException("Dance Style not found with id: " + danceStyleId));
    }
}

