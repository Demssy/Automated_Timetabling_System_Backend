package com.timetable.backend.domain.repository;

import com.timetable.backend.domain.model.AbstractUser;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AbstractUser, Long> {
    Optional<AbstractUser> findByEmail(String email);
    boolean existsByEmail(String email);

    /**
     * Finds users whose email contains the given query string (case-insensitive).
     * Pageable is used to limit the number of autocomplete suggestions.
     */
    List<AbstractUser> findByEmailContainingIgnoreCase(String emailQuery, Pageable pageable);
}
