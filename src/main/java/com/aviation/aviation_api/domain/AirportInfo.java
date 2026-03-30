package com.aviation.aviation_api.domain;

import lombok.Builder;

/**
 * Immutable representation of airport details returned by the API.
 */
@Builder
public record AirportInfo(
        String icaoCode,
        String iataCode,
        String name,
        String country,
        String state,
        Double latitude,
        Double longitude,
        Integer elevationFt,
        String source
) {}
