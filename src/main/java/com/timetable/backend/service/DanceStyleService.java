package com.timetable.backend.service;

import com.timetable.backend.domain.dto.DanceStyleDTO;
import com.timetable.backend.domain.mapper.DictionaryMapper;
import com.timetable.backend.domain.model.DanceStyle;
import com.timetable.backend.domain.repository.DanceStyleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing DanceStyle entities.
 */
@Service
@RequiredArgsConstructor
public class DanceStyleService {

    private final DanceStyleRepository danceStyleRepository;
    private final DictionaryMapper dictionaryMapper;

    @Transactional(readOnly = true)
    public List<DanceStyleDTO> getAllStyles() {
        return danceStyleRepository.findAll().stream()
                .map(dictionaryMapper::toDanceStyleDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public DanceStyleDTO getStyleById(Long id) {
        DanceStyle style = danceStyleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dance style not found with id: " + id));
        return dictionaryMapper.toDanceStyleDTO(style);
    }

    @Transactional
    public DanceStyleDTO createStyle(DanceStyleDTO dto) {
        danceStyleRepository.findByName(dto.name())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Dance style with name '" + dto.name() + "' already exists");
                });

        DanceStyle saved = danceStyleRepository.save(dictionaryMapper.toDanceStyle(dto));
        return dictionaryMapper.toDanceStyleDTO(saved);
    }

    @Transactional
    public DanceStyleDTO updateStyle(Long id, DanceStyleDTO dto) {
        DanceStyle style = danceStyleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dance style not found with id: " + id));

        if (!style.getName().equals(dto.name())) {
            danceStyleRepository.findByName(dto.name())
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Dance style with name '" + dto.name() + "' already exists");
                    });
        }

        style.setName(dto.name());
        DanceStyle saved = danceStyleRepository.save(style);
        return dictionaryMapper.toDanceStyleDTO(saved);
    }

    @Transactional
    public void deleteStyle(Long id) {
        if (!danceStyleRepository.existsById(id)) {
            throw new IllegalArgumentException("Dance style not found with id: " + id);
        }
        danceStyleRepository.deleteById(id);
    }
}

