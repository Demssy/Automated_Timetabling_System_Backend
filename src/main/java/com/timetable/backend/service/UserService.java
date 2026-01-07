package com.timetable.backend.service;

import com.timetable.backend.domain.dto.UserResponse;
import com.timetable.backend.domain.model.AbstractUser;
import com.timetable.backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing user-related operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

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

        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getRole().getName(),
            user.isActive()
        );
    }
}

