package com.aviation.aviation_api.provider;

import com.aviation.aviation_api.domain.AirportInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.Optional;

/**
 * Fallback provider — queries the AirportsAPI REST endpoint.
 */
@Slf4j
@Component
@Order(2)
public class AirportsApiProvider implements AirportDataProvider {

    private final RestClient restClient;

    public AirportsApiProvider(RestClient.Builder builder,
                               @Value("${airports-api.base-url:https://airportsapi.com/api}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public Optional<AirportInfo> fetchAirport(String icaoCode) {
        log.info("Fetching airport {} from AirportsAPI", icaoCode);
        JsonNode response = restClient.get()
                .uri("/airports/{code}", icaoCode)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.has("data")) {
            return Optional.empty();
        }

        JsonNode attrs = response.get("data").get("attributes");
        return Optional.of(AirportInfo.builder()
                .icaoCode(text(attrs, "icao_code"))
                .iataCode(text(attrs, "iata_code"))
                .name(text(attrs, "name"))
                .latitude(attrs.has("latitude") ? attrs.get("latitude").asDouble() : null)
                .longitude(attrs.has("longitude") ? attrs.get("longitude").asDouble() : null)
                .elevationFt(attrs.has("elevation") ? attrs.get("elevation").asInt() : null)
                .source(name())
                .build());
    }

    @Override
    public String name() {
        return "AirportsAPI";
    }

    private String text(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }
}
