package com.timetable.backend.service;

import com.timetable.backend.domain.dto.UpdateUserRequest;
import com.timetable.backend.domain.dto.UserResponse;
import com.timetable.backend.domain.model.AbstractUser;
import com.timetable.backend.domain.model.Role;
import com.timetable.backend.domain.repository.RoleRepository;
import com.timetable.backend.domain.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.data.domain.PageRequest;

/**
 * Service for managing user-related operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /**
     * Retrieves user information by email (username).
     *
     * @param email the email address of the authenticated user
     * @return UserResponse containing user details
     * @throws UsernameNotFoundException if user is not found
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserInfo(String email) {
        log.debug("Fetching user info for email: {}", email);

        AbstractUser user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return mapToUserResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .toList();
    }

    /**
     * Searches users by partial email match for autocomplete input.
     * Returns at most {@code limit} results (max 20).
     *
     * @param emailQuery partial email string entered by the user
     * @param limit      maximum number of suggestions to return
     * @return list of matching UserResponse objects
     */
    @Transactional(readOnly = true)
    public List<UserResponse> searchUsersByEmail(String emailQuery, int limit) {
        int safeLimit = Math.min(limit, 20);
        log.debug("Searching users by email query='{}', limit={}", emailQuery, safeLimit);

        return userRepository
                .findByEmailContainingIgnoreCase(emailQuery, PageRequest.of(0, safeLimit))
                .stream()
                .map(this::mapToUserResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        AbstractUser user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        AbstractUser user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use");
        }

        user.setEmail(request.email());
        user.setFullName(request.fullName());

        if (request.isActive() != null) {
            user.setActive(request.isActive());
        }

        if (request.role() != null) {
            Role role = roleRepository.findByName(request.role())
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: " + request.role()));
            user.setRole(role);
        }

        return mapToUserResponse(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    private UserResponse mapToUserResponse(AbstractUser user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().getName(),
                user.isActive()
        );
    }
}
