package com.aviation.aviation_api.controller;

import com.aviation.aviation_api.domain.AirportInfo;
import com.aviation.aviation_api.service.AirportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for airport lookups by ICAO code.
 */
@RestController
@RequestMapping("/api/airports")
@RequiredArgsConstructor
public class AirportController {

    private final AirportService airportService;

    /**
     * Retrieve airport details by ICAO code.
     *
     * @param icaoCode 4-letter ICAO airport identifier (e.g. KJFK, EGLL)
     * @return airport details
     */
    @GetMapping("/{icaoCode}")
    public ResponseEntity<AirportInfo> getAirport(@PathVariable String icaoCode) {
        return ResponseEntity.ok(airportService.getAirport(icaoCode));
    }
}
