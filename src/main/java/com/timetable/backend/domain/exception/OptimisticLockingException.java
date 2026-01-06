package com.timetable.backend.domain.exception;

/**
 * Exception thrown when an optimistic locking conflict occurs.
 * This happens when two users try to update the same entity simultaneously.
 * Results in HTTP 409 Conflict response.
 *
 * The client should refresh the data and retry the operation.
 */
public class OptimisticLockingException extends RuntimeException {

    public OptimisticLockingException(String message) {
        super(message);
    }

    public OptimisticLockingException(String resourceName, Long id) {
        super(String.format(
            "%s with id %d was modified by another user. Please refresh and try again.",
            resourceName, id
        ));
    }
}

