package com.timetable.backend.service;

import com.timetable.backend.domain.dto.WeeklyAvailabilityDTO;
import com.timetable.backend.domain.dto.WeeklyAvailabilityRequest;
import com.timetable.backend.domain.model.AbstractUser;
import com.timetable.backend.domain.model.AvailabilityRole;
import com.timetable.backend.domain.model.WeeklyAvailability;
import com.timetable.backend.domain.repository.UserRepository;
import com.timetable.backend.domain.repository.WeeklyAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WeeklyAvailabilityService {

    private final WeeklyAvailabilityRepository repository;
    private final UserRepository userRepository;

    /** Returns the user's slots for all roles; each DTO carries its availabilityRole. */
    @Transactional(readOnly = true)
    public List<WeeklyAvailabilityDTO> getByUserId(Long userId) {
        return repository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WeeklyAvailabilityDTO> getByUserId(Long userId, AvailabilityRole role) {
        return repository.findByUserIdAndAvailabilityRole(userId, role).stream()
                .map(this::mapToDTO)
                .toList();
    }

    /**
     * Fully replaces the weekly availability schedule for a specific user.
     * Only the roles present in the DTOs are replaced (the default role if the list is empty),
     * so the other role's schedule of a dual-role user stays untouched.
     */
    @Transactional
    public void updateUserSchedule(Long userId, List<WeeklyAvailabilityDTO> dtos) {
        AbstractUser user = loadUser(userId);

        Set<AvailabilityRole> roles = EnumSet.noneOf(AvailabilityRole.class);
        if (dtos == null || dtos.isEmpty()) {
            roles.add(AvailabilityRole.defaultFor(user));
        } else {
            dtos.forEach(dto -> roles.add(resolveRole(user, dto.availabilityRole())));
        }
        roles.forEach(role -> repository.deleteByUserIdAndAvailabilityRole(userId, role));

        if (dtos != null && !dtos.isEmpty()) {
            List<WeeklyAvailability> entities = dtos.stream()
                    .map(dto -> toEntity(user, dto, resolveRole(user, dto.availabilityRole())))
                    .toList();
            repository.saveAll(entities);
        }
    }

    /** Fully replaces the weekly schedule of a user for one explicit role. */
    @Transactional
    public void updateUserSchedule(Long userId, AvailabilityRole role, List<WeeklyAvailabilityDTO> dtos) {
        AbstractUser user = loadUser(userId);
        requireRole(user, role);

        repository.deleteByUserIdAndAvailabilityRole(userId, role);
        if (dtos != null && !dtos.isEmpty()) {
            repository.saveAll(dtos.stream().map(dto -> toEntity(user, dto, role)).toList());
        }
    }

    /**
     * Creates a single weekly availability slot for the given user.
     *
     * @param userId  target user ID
     * @param request slot data (dayOfWeek, startTime, endTime, optional availabilityRole)
     * @return saved slot as DTO
     */
    @Transactional
    public WeeklyAvailabilityDTO createSlot(Long userId, WeeklyAvailabilityRequest request) {
        AbstractUser user = loadUser(userId);

        WeeklyAvailability entity = new WeeklyAvailability();
        entity.setUser(user);
        entity.setDayOfWeek(request.dayOfWeek());
        entity.setStartTime(request.startTime());
        entity.setEndTime(request.endTime());
        entity.setAvailabilityRole(resolveRole(user, request.availabilityRole()));

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
        if (request.availabilityRole() != null) {
            requireRole(entity.getUser(), request.availabilityRole());
            entity.setAvailabilityRole(request.availabilityRole());
        }

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
                entity.getEndTime(),
                entity.getAvailabilityRole()
        );
    }

    private WeeklyAvailability toEntity(AbstractUser user, WeeklyAvailabilityDTO dto, AvailabilityRole role) {
        WeeklyAvailability entity = new WeeklyAvailability();
        entity.setUser(user);
        entity.setDayOfWeek(dto.dayOfWeek());
        entity.setStartTime(dto.startTime());
        entity.setEndTime(dto.endTime());
        entity.setAvailabilityRole(role);
        return entity;
    }

    private AbstractUser loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    /** Uses the requested role (validated against the user's roles) or falls back to the default one. */
    private AvailabilityRole resolveRole(AbstractUser user, AvailabilityRole requested) {
        if (requested == null) {
            return AvailabilityRole.defaultFor(user);
        }
        requireRole(user, requested);
        return requested;
    }

    private void requireRole(AbstractUser user, AvailabilityRole role) {
        if (!user.hasRole(role.name())) {
            throw new IllegalArgumentException("User does not have role " + role.name());
        }
    }
}