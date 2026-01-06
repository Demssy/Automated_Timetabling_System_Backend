package com.timetable.backend.service;

import java.time.LocalDate;

/**
 * Service interface for authentication and user registration operations.
 * Handles JWT token generation and user authentication.
 */
public interface IAuthService {

    /**
     * Registers a new student user in the system.
     *
     * @param email the student's email address
     * @param password the student's password (will be encoded)
     * @param fullName the student's full name
     * @param birthDate the student's date of birth
     * @return JWT token for the newly registered student
     * @throws com.timetable.backend.domain.exception.BusinessRuleViolationException if email already exists
     */
    String registerStudent(String email, String password, String fullName, LocalDate birthDate);

    /**
     * Authenticates a user with email and password.
     *
     * @param email the user's email
     * @param password the user's password
     * @return JWT token for the authenticated user
     * @throws org.springframework.security.authentication.BadCredentialsException if credentials are invalid
     */
    String authenticate(String email, String password);
}

