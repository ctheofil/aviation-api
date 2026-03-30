package com.aviation.aviation_api.provider;

import com.aviation.aviation_api.domain.AirportInfo;
import tools.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Primary provider — queries the FAA/NOAA Aviation Weather API.
 */
@Slf4j
@Component
@Order(1)
public class AviationWeatherProvider implements AirportDataProvider {

    private final RestClient restClient;

    public AviationWeatherProvider(RestClient.Builder builder,
                                   @Value("${aviation-weather.base-url:https://aviationweather.gov/api/data}") String baseUrl) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public Optional<AirportInfo> fetchAirport(String icaoCode) {
        log.info("Fetching airport {} from AviationWeather API", icaoCode);
        JsonNode[] response = restClient.get()
                .uri("/airport?ids={code}&format=json", icaoCode)
                .retrieve()
                .body(JsonNode[].class);

        if (response == null || response.length == 0) {
            return Optional.empty();
        }

        JsonNode node = response[0];
        return Optional.of(AirportInfo.builder()
                .icaoCode(text(node, "icaoId"))
                .iataCode(text(node, "iataId"))
                .name(trimmed(node, "name"))
                .country(text(node, "country"))
                .state(text(node, "state"))
                .latitude(node.has("lat") ? node.get("lat").asDouble() : null)
                .longitude(node.has("lon") ? node.get("lon").asDouble() : null)
                .elevationFt(node.has("elev") ? node.get("elev").asInt() : null)
                .source(name())
                .build());
    }

    @Override
    public String name() {
        return "AviationWeather.gov";
    }

    private String text(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private String trimmed(JsonNode node, String field) {
        String val = text(node, field);
        return val != null ? val.trim() : null;
    }
}
