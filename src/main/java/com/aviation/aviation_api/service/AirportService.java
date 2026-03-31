package com.aviation.aviation_api.service;

import com.aviation.aviation_api.domain.AirportInfo;
import com.aviation.aviation_api.exception.AirportNotFoundException;
import com.aviation.aviation_api.exception.UpstreamServiceException;
import com.aviation.aviation_api.provider.AirportDataProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates airport lookups across ordered providers.
 * Applies caching, retry, and circuit breaker via Resilience4j.
 */
@Slf4j
@Service
public class AirportService {

    private final List<AirportDataProvider> providers;

    public AirportService(List<AirportDataProvider> providers) {
        this.providers = providers;
        log.info("Registered airport data providers: {}",
                providers.stream().map(AirportDataProvider::name).toList());
    }

    /**
     * Looks up airport by ICAO code, trying each registered provider in order.
     * Results are cached in-memory. Circuit breaker and retry are applied.
     */
    @Cacheable(value = "airports", key = "#icaoCode.toUpperCase()")
    @CircuitBreaker(name = "airportLookup", fallbackMethod = "fallbackLookup")
    @Retry(name = "airportLookup")
    public AirportInfo getAirport(String icaoCode) {
        return tryProviders(icaoCode);
    }

    /**
     * Fallback when circuit breaker is open — logs and rethrows as upstream failure.
     */
    private AirportInfo fallbackLookup(String icaoCode, Throwable t) {
        if (t instanceof AirportNotFoundException e) {
            throw e;
        }
        log.error("All providers failed for ICAO code {}: {}", icaoCode, t.getMessage());
        throw new UpstreamServiceException("All upstream providers are unavailable");
    }

    private AirportInfo tryProviders(String icaoCode) {
        boolean allFailed = true;
        for (AirportDataProvider provider : providers) {
            try {
                var result = provider.fetchAirport(icaoCode.toUpperCase());
                if (result.isPresent()) {
                    log.info("Airport {} resolved via {}", icaoCode, provider.name());
                    return result.get();
                }
                allFailed = false; // provider responded successfully but had no data
            } catch (Exception e) {
                log.warn("Provider {} failed for {}: {}", provider.name(), icaoCode, e.getMessage());
            }
        }
        if (allFailed) {
            throw new UpstreamServiceException("All upstream providers are unavailable");
        }
        throw new AirportNotFoundException(icaoCode);
    }
}
