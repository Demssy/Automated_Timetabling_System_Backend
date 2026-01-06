package com.timetable.backend.domain.exception;

/**
 * Exception thrown when a requested resource is not found in the database.
 * Results in HTTP 404 Not Found response.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructor for resource not found by ID.
     *
     * @param resourceName the name of the resource (e.g., "Teacher", "Room")
     * @param id the ID that was not found
     */
    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s with id %d not found", resourceName, id));
    }

    /**
     * Constructor for resource not found by field.
     *
     * @param resourceName the name of the resource
     * @param fieldName the field name (e.g., "email", "name")
     * @param fieldValue the field value
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s with %s '%s' not found", resourceName, fieldName, fieldValue));
    }
}
