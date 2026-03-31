package com.aviation.aviation_api.integration;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WireMockTest(httpPort = 9999)
class AirportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String AVIATION_WEATHER_JSON = """
            [{"icaoId":"KJFK","iataId":"JFK","name":"JFK INTL ","country":"US","state":"NY","lat":40.6399,"lon":-73.7787,"elev":4}]
            """;

    private static final String AIRPORTS_API_JSON = """
            {"data":{"id":"EGLL","type":"airports","attributes":{"name":"Heathrow Airport","icao_code":"EGLL","iata_code":"LHR","latitude":51.4706,"longitude":-0.461941,"elevation":83}}}
            """;

    @DynamicPropertySource
    static void configureWireMock(DynamicPropertyRegistry registry) {
        registry.add("aviation-weather.base-url", () -> "http://localhost:9999");
        registry.add("airports-api.base-url", () -> "http://localhost:9999");
    }

    @Test
    void shouldReturnAirportFromPrimaryProvider(WireMockRuntimeInfo wm) throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/airport"))
                .withQueryParam("ids", equalTo("KJFK"))
                .withQueryParam("format", equalTo("json"))
                .willReturn(okJson(AVIATION_WEATHER_JSON)));

        mockMvc.perform(get("/api/airports/KJFK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.icaoCode").value("KJFK"))
                .andExpect(jsonPath("$.iataCode").value("JFK"))
                .andExpect(jsonPath("$.name").value("JFK INTL"))
                .andExpect(jsonPath("$.source").value("AviationWeather.gov"));
    }

    @Test
    void shouldFallbackToSecondProvider(WireMockRuntimeInfo wm) throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/airport"))
                .withQueryParam("ids", equalTo("EGLL"))
                .withQueryParam("format", equalTo("json"))
                .willReturn(okJson("[]")));

        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/airports/EGLL"))
                .willReturn(okJson(AIRPORTS_API_JSON)));

        mockMvc.perform(get("/api/airports/EGLL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.icaoCode").value("EGLL"))
                .andExpect(jsonPath("$.iataCode").value("LHR"))
                .andExpect(jsonPath("$.name").value("Heathrow Airport"))
                .andExpect(jsonPath("$.source").value("AirportsAPI"));
    }

    @Test
    void shouldReturn404WhenAirportNotFound(WireMockRuntimeInfo wm) throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/airport"))
                .withQueryParam("ids", equalTo("XXXX"))
                .withQueryParam("format", equalTo("json"))
                .willReturn(okJson("[]")));

        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/airports/XXXX"))
                .willReturn(aResponse().withStatus(404)));

        mockMvc.perform(get("/api/airports/XXXX"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenAllUpstreamsError(WireMockRuntimeInfo wm) throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/airport"))
                .willReturn(serverError()));

        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathMatching("/airports/.*"))
                .willReturn(serverError()));

        mockMvc.perform(get("/api/airports/ZZZZ"))
                .andExpect(status().isNotFound());
    }
}
