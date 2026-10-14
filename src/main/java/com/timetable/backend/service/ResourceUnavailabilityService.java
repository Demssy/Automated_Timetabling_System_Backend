package com.timetable.backend.service;

import com.timetable.backend.domain.dto.ResourceUnavailabilityDTO;
import com.timetable.backend.domain.model.AbstractUser;
import com.timetable.backend.domain.model.AvailabilityRole;
import com.timetable.backend.domain.model.ResourceUnavailability;
import com.timetable.backend.domain.repository.ResourceUnavailabilityRepository;
import com.timetable.backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ResourceUnavailabilityService {

    private final ResourceUnavailabilityRepository repository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ResourceUnavailabilityDTO> getByUserId(Long userId) {
        return repository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResourceUnavailabilityDTO> getByUserId(Long userId, AvailabilityRole role) {
        return repository.findByUserIdAndAvailabilityRole(userId, role).stream()
                .map(this::mapToDTO)
                .toList();
    }

    /**
     * Fully replaces one-time exceptions for a specific user.
     * Only the roles present in the DTOs are replaced (the default role if the list is empty),
     * so the other role's exceptions of a dual-role user stay untouched.
     */
    @Transactional
    public void updateUserExceptions(Long userId, List<ResourceUnavailabilityDTO> dtos) {
        AbstractUser user = loadUser(userId);

        Set<AvailabilityRole> roles = EnumSet.noneOf(AvailabilityRole.class);
        if (dtos == null || dtos.isEmpty()) {
            roles.add(AvailabilityRole.defaultFor(user));
        } else {
            dtos.forEach(dto -> roles.add(resolveRole(user, dto.availabilityRole())));
        }
        roles.forEach(role -> repository.deleteByUserIdAndAvailabilityRole(userId, role));

        if (dtos != null && !dtos.isEmpty()) {
            List<ResourceUnavailability> entities = dtos.stream()
                    .map(dto -> toEntity(user, dto, resolveRole(user, dto.availabilityRole())))
                    .toList();
            repository.saveAll(entities);
        }
    }

    /** Fully replaces one-time exceptions of a user for one explicit role. */
    @Transactional
    public void updateUserExceptions(Long userId, AvailabilityRole role, List<ResourceUnavailabilityDTO> dtos) {
        AbstractUser user = loadUser(userId);
        requireRole(user, role);

        repository.deleteByUserIdAndAvailabilityRole(userId, role);
        if (dtos != null && !dtos.isEmpty()) {
            repository.saveAll(dtos.stream().map(dto -> toEntity(user, dto, role)).toList());
        }
    }

    private ResourceUnavailability toEntity(AbstractUser user, ResourceUnavailabilityDTO dto, AvailabilityRole role) {
        ResourceUnavailability entity = new ResourceUnavailability();
        entity.setUser(user);
        entity.setDate(dto.date());
        entity.setStartTime(dto.startTime());
        entity.setEndTime(dto.endTime());
        entity.setReason(dto.reason());
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

    private ResourceUnavailabilityDTO mapToDTO(ResourceUnavailability entity) {
        return new ResourceUnavailabilityDTO(
                entity.getId(),
                entity.getDate(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getReason(),
                entity.getAvailabilityRole()
        );
    }
}