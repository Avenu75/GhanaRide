package com.ghanaride.exception;

/**
 * Thrown when a requested resource doesn't exist.
 * Used by services to signal 404-equivalent errors.
 */
public class ResourceNotFoundException
        extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(
            String resource, Long id
    ) {
        super(resource + " not found with id: " + id);
    }

    public ResourceNotFoundException(
            String resource, String field, Object value
    ) {
        super(resource + " not found with " +
                field + ": " + value);
    }
}