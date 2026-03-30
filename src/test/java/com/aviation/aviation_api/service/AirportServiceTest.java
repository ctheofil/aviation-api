package com.aviation.aviation_api.service;

import com.aviation.aviation_api.domain.AirportInfo;
import com.aviation.aviation_api.exception.AirportNotFoundException;
import com.aviation.aviation_api.provider.AirportDataProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AirportServiceTest {

    private static final AirportInfo SAMPLE = AirportInfo.builder()
            .icaoCode("KJFK").name("JFK INTL").source("TestProvider").build();

    @Test
    void shouldReturnAirportFromFirstProvider() {
        var provider = mockProvider("Primary", Optional.of(SAMPLE));
        var service = new AirportService(List.of(provider));

        var result = service.getAirport("KJFK");

        assertThat(result.icaoCode()).isEqualTo("KJFK");
        verify(provider).fetchAirport("KJFK");
    }

    @Test
    void shouldFallToSecondProviderWhenFirstReturnsEmpty() {
        var primary = mockProvider("Primary", Optional.empty());
        var fallback = mockProvider("Fallback", Optional.of(SAMPLE));
        var service = new AirportService(List.of(primary, fallback));

        var result = service.getAirport("KJFK");

        assertThat(result.icaoCode()).isEqualTo("KJFK");
        verify(primary).fetchAirport("KJFK");
        verify(fallback).fetchAirport("KJFK");
    }

    @Test
    void shouldFallToSecondProviderWhenFirstThrows() {
        var primary = mock(AirportDataProvider.class);
        when(primary.name()).thenReturn("Primary");
        when(primary.fetchAirport(any())).thenThrow(new RuntimeException("timeout"));

        var fallback = mockProvider("Fallback", Optional.of(SAMPLE));
        var service = new AirportService(List.of(primary, fallback));

        var result = service.getAirport("KJFK");

        assertThat(result.icaoCode()).isEqualTo("KJFK");
    }

    @Test
    void shouldThrowNotFoundWhenAllProvidersReturnEmpty() {
        var provider = mockProvider("Primary", Optional.empty());
        var service = new AirportService(List.of(provider));

        assertThatThrownBy(() -> service.getAirport("XXXX"))
                .isInstanceOf(AirportNotFoundException.class);
    }

    @Test
    void shouldUppercaseIcaoCode() {
        var provider = mockProvider("Primary", Optional.of(SAMPLE));
        var service = new AirportService(List.of(provider));

        service.getAirport("kjfk");

        verify(provider).fetchAirport("KJFK");
    }

    private AirportDataProvider mockProvider(String name, Optional<AirportInfo> result) {
        var provider = mock(AirportDataProvider.class);
        when(provider.name()).thenReturn(name);
        when(provider.fetchAirport(any())).thenReturn(result);
        return provider;
    }
}
