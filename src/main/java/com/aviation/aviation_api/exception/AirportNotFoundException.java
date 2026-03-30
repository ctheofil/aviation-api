package com.aviation.aviation_api.exception;

/**
 * Thrown when no provider returns a result for the given ICAO code.
 */
public class AirportNotFoundException extends RuntimeException {
    public AirportNotFoundException(String icaoCode) {
        super("Airport not found for ICAO code: " + icaoCode);
    }
}
