package com.timetable.backend.service;

import com.timetable.backend.domain.dto.ScheduleMetadataDTO;
import com.timetable.backend.domain.mapper.ScheduleMetadataMapper;
import com.timetable.backend.domain.model.ScheduleMetadata;
import com.timetable.backend.domain.model.ScheduleStatus;
import com.timetable.backend.domain.repository.ScheduleMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleMetadataService {

    private final ScheduleMetadataRepository repository;
    private final ScheduleMetadataMapper mapper;

    @Transactional(readOnly = true)
    public List<ScheduleMetadataDTO> getAll() {
        return repository.findAll().stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScheduleMetadataDTO getById(Long id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found with id: " + id));
    }

    @Transactional
    public ScheduleMetadataDTO create(ScheduleMetadataDTO dto) {
        ScheduleMetadata entity = mapper.toEntity(dto);
        if (entity.getStatus() == null) {
            entity.setStatus(ScheduleStatus.DRAFT);
        }
        return mapper.toDTO(repository.save(entity));
    }

    @Transactional
    public ScheduleMetadataDTO update(Long id, ScheduleMetadataDTO dto) {
        ScheduleMetadata entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found with id: " + id));

        entity.setName(dto.name());
        entity.setValidFrom(dto.validFrom());
        entity.setValidTo(dto.validTo());

        if (dto.status() != null) {
            entity.setStatus(dto.status());
        }

        return mapper.toDTO(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Schedule not found with id: " + id);
        }
        repository.deleteById(id);
    }
}

