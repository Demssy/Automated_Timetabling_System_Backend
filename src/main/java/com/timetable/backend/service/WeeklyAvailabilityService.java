package com.timetable.backend.service;

import com.timetable.backend.domain.dto.WeeklyAvailabilityDTO;
import com.timetable.backend.domain.dto.WeeklyAvailabilityRequest;
import com.timetable.backend.domain.model.AbstractUser;
import com.timetable.backend.domain.model.WeeklyAvailability;
import com.timetable.backend.domain.repository.UserRepository;
import com.timetable.backend.domain.repository.WeeklyAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WeeklyAvailabilityService {

    private final WeeklyAvailabilityRepository repository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<WeeklyAvailabilityDTO> getByUserId(Long userId) {
        return repository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .toList();
    }

    /**
     * Fully replaces the weekly availability schedule for a specific user.
     */
    @Transactional
    public void updateUserSchedule(Long userId, List<WeeklyAvailabilityDTO> dtos) {
        AbstractUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Clear old schedule
        repository.deleteByUserId(userId);

        // Save new schedule
        if (dtos != null && !dtos.isEmpty()) {
            List<WeeklyAvailability> entities = dtos.stream()
                    .map(dto -> {
                        WeeklyAvailability entity = new WeeklyAvailability();
                        entity.setUser(user);
                        entity.setDayOfWeek(dto.dayOfWeek());
                        entity.setStartTime(dto.startTime());
                        entity.setEndTime(dto.endTime());
                        return entity;
                    })
                    .toList();
            repository.saveAll(entities);
        }
    }

    /**
     * Creates a single weekly availability slot for the given user.
     *
     * @param userId  target user ID
     * @param request slot data (dayOfWeek, startTime, endTime)
     * @return saved slot as DTO
     */
    @Transactional
    public WeeklyAvailabilityDTO createSlot(Long userId, WeeklyAvailabilityRequest request) {
        AbstractUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        WeeklyAvailability entity = new WeeklyAvailability();
        entity.setUser(user);
        entity.setDayOfWeek(request.dayOfWeek());
        entity.setStartTime(request.startTime());
        entity.setEndTime(request.endTime());

        return mapToDTO(repository.save(entity));
    }

    /**
     * Updates an existing weekly availability slot by its ID.
     *
     * @param slotId  ID of the slot to update
     * @param request new values (dayOfWeek, startTime, endTime)
     * @return updated slot as DTO
     */
    @Transactional
    public WeeklyAvailabilityDTO updateSlot(Long slotId, WeeklyAvailabilityRequest request) {
        WeeklyAvailability entity = repository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Availability slot not found: " + slotId));

        entity.setDayOfWeek(request.dayOfWeek());
        entity.setStartTime(request.startTime());
        entity.setEndTime(request.endTime());

        return mapToDTO(repository.save(entity));
    }

    /**
     * Deletes a single weekly availability slot by its ID.
     *
     * @param slotId ID of the slot to delete
     */
    @Transactional
    public void deleteSlot(Long slotId) {
        if (!repository.existsById(slotId)) {
            throw new IllegalArgumentException("Availability slot not found: " + slotId);
        }
        repository.deleteById(slotId);
    }

    private WeeklyAvailabilityDTO mapToDTO(WeeklyAvailability entity) {
        return new WeeklyAvailabilityDTO(
                entity.getId(),
                entity.getDayOfWeek(),
                entity.getStartTime(),
                entity.getEndTime()
        );
    }
}