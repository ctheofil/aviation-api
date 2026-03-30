package com.aviation.aviation_api.exception;

/**
 * Thrown when all upstream providers are unavailable (e.g. circuit breaker open).
 */
public class UpstreamServiceException extends RuntimeException {
    public UpstreamServiceException(String message) {
        super(message);
    }
}
