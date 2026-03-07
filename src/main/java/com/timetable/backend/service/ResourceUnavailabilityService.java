package com.timetable.backend.service;

import com.timetable.backend.domain.dto.ResourceUnavailabilityDTO;
import com.timetable.backend.domain.mapper.ResourceUnavailabilityMapper;
import com.timetable.backend.domain.model.ResourceUnavailability;
import com.timetable.backend.domain.model.Teacher;
import com.timetable.backend.domain.model.Timeslot;
import com.timetable.backend.domain.repository.ResourceUnavailabilityRepository;
import com.timetable.backend.domain.repository.TeacherRepository;
import com.timetable.backend.domain.repository.TimeslotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceUnavailabilityService {

    private final ResourceUnavailabilityRepository repository;
    private final TeacherRepository teacherRepository;
    private final TimeslotRepository timeslotRepository;
    private final ResourceUnavailabilityMapper mapper;

    @Transactional(readOnly = true)
    public List<ResourceUnavailabilityDTO> getAll() {
        return repository.findAll().stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResourceUnavailabilityDTO getById(Long id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("ResourceUnavailability not found with id: " + id));
    }

    @Transactional
    public ResourceUnavailabilityDTO create(ResourceUnavailabilityDTO dto) {
        Teacher teacher = teacherRepository.findById(dto.teacherId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        Timeslot timeslot = timeslotRepository.findById(dto.timeslotId())
                .orElseThrow(() -> new IllegalArgumentException("Timeslot not found"));

        ResourceUnavailability entity = mapper.toEntity(dto);
        entity.setTeacher(teacher);
        entity.setTimeslot(timeslot);

        return mapper.toDTO(repository.save(entity));
    }

    @Transactional
    public ResourceUnavailabilityDTO update(Long id, ResourceUnavailabilityDTO dto) {
        ResourceUnavailability entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ResourceUnavailability not found with id: " + id));

        if (!entity.getTeacher().getId().equals(dto.teacherId())) {
             Teacher teacher = teacherRepository.findById(dto.teacherId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
             entity.setTeacher(teacher);
        }

        if (!entity.getTimeslot().getId().equals(dto.timeslotId())) {
             Timeslot timeslot = timeslotRepository.findById(dto.timeslotId())
                .orElseThrow(() -> new IllegalArgumentException("Timeslot not found"));
             entity.setTimeslot(timeslot);
        }

        entity.setReason(dto.reason());

        return mapper.toDTO(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("ResourceUnavailability not found with id: " + id);
        }
        repository.deleteById(id);
    }
}

