package com.timetable.backend.service;

import com.timetable.backend.domain.dto.WeeklyAvailabilityDTO;
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

    private WeeklyAvailabilityDTO mapToDTO(WeeklyAvailability entity) {
        return new WeeklyAvailabilityDTO(
                entity.getId(),
                entity.getDayOfWeek(),
                entity.getStartTime(),
                entity.getEndTime()
        );
    }
}