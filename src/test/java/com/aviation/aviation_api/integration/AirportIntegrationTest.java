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

    private static final String AIRPORT_JSON = """
            [{"icaoId":"KJFK","iataId":"JFK","name":"JFK INTL ","country":"US","state":"NY","lat":40.6399,"lon":-73.7787,"elev":4}]
            """;

    @DynamicPropertySource
    static void configureWireMock(DynamicPropertyRegistry registry) {
        registry.add("aviation-weather.base-url", () -> "http://localhost:9999");
    }

    @Test
    void shouldReturnAirportFromUpstream(WireMockRuntimeInfo wm) throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/airport"))
                .withQueryParam("ids", equalTo("KJFK"))
                .withQueryParam("format", equalTo("json"))
                .willReturn(okJson(AIRPORT_JSON)));

        mockMvc.perform(get("/api/airports/KJFK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.icaoCode").value("KJFK"))
                .andExpect(jsonPath("$.iataCode").value("JFK"))
                .andExpect(jsonPath("$.name").value("JFK INTL"))
                .andExpect(jsonPath("$.latitude").value(40.6399))
                .andExpect(jsonPath("$.source").value("AviationWeather.gov"));
    }

    @Test
    void shouldReturn404WhenAirportNotFound(WireMockRuntimeInfo wm) throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/airport"))
                .withQueryParam("ids", equalTo("XXXX"))
                .withQueryParam("format", equalTo("json"))
                .willReturn(okJson("[]")));

        mockMvc.perform(get("/api/airports/XXXX"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenUpstreamReturnsError(WireMockRuntimeInfo wm) throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/airport"))
                .willReturn(serverError()));

        mockMvc.perform(get("/api/airports/ZZZZ"))
                .andExpect(status().isNotFound());
    }
}
