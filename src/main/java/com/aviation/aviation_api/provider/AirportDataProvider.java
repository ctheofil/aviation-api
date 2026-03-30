package com.aviation.aviation_api.provider;

import com.aviation.aviation_api.domain.AirportInfo;

import java.util.Optional;

/**
 * Abstraction for airport data providers.
 * Implementations fetch airport details from different upstream sources.
 */
public interface AirportDataProvider {

    Optional<AirportInfo> fetchAirport(String icaoCode);

    String name();
}
