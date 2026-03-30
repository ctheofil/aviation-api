package com.aviation.aviation_api.controller;

import com.aviation.aviation_api.domain.AirportInfo;
import com.aviation.aviation_api.exception.AirportNotFoundException;
import com.aviation.aviation_api.exception.GlobalExceptionHandler;
import com.aviation.aviation_api.exception.UpstreamServiceException;
import com.aviation.aviation_api.service.AirportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AirportController.class)
@Import(GlobalExceptionHandler.class)
class AirportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AirportService airportService;

    @TestConfiguration
    static class Config {
        @Bean
        AirportService airportService() {
            return mock(AirportService.class);
        }
    }

    @Test
    void shouldReturnAirportWhenFound() throws Exception {
        var airport = AirportInfo.builder()
                .icaoCode("KJFK").iataCode("JFK").name("JFK INTL")
                .country("US").state("NY")
                .latitude(40.6399).longitude(-73.7787)
                .elevationFt(4).source("AviationWeather.gov")
                .build();

        when(airportService.getAirport("KJFK")).thenReturn(airport);

        mockMvc.perform(get("/api/airports/KJFK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.icaoCode").value("KJFK"))
                .andExpect(jsonPath("$.iataCode").value("JFK"))
                .andExpect(jsonPath("$.name").value("JFK INTL"))
                .andExpect(jsonPath("$.country").value("US"));
    }

    @Test
    void shouldReturn404WhenAirportNotFound() throws Exception {
        when(airportService.getAirport("XXXX")).thenThrow(new AirportNotFoundException("XXXX"));

        mockMvc.perform(get("/api/airports/XXXX"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Airport not found for ICAO code: XXXX"));
    }

    @Test
    void shouldReturn503WhenUpstreamFails() throws Exception {
        when(airportService.getAirport("KJFK"))
                .thenThrow(new UpstreamServiceException("All upstream providers are unavailable"));

        mockMvc.perform(get("/api/airports/KJFK"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("All upstream providers are unavailable"));
    }
}
