package com.timetable.backend.domain.exception;

/**
 * Exception thrown when a business rule is violated.
 * Results in HTTP 400 Bad Request response.
 *
 * Examples:
 * - Email already in use
 * - Invalid dance style IDs
 * - Teacher is already booked for this timeslot
 */
public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }

    public BusinessRuleViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}

