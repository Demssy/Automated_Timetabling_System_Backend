package com.timetable.backend.domain.repository;

import com.timetable.backend.domain.model.AbstractUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AbstractUser, Long> {
    Optional<AbstractUser> findByEmail(String email);
}

