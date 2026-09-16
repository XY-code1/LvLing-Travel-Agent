package com.guido.scenicai.integration.amap;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AmapWeatherProvider {
    private final AmapWebServiceClient client;

    public Weather current(String city) {
        JsonNode geocode = client.get("/v3/geocode/geo", Map.of("address", city, "city", city))
                .path("geocodes").path(0);
        String adcode = geocode.path("adcode").asText();
        if (adcode.isBlank()) return null;
        JsonNode live = client.get("/v3/weather/weatherInfo", Map.of("city", adcode, "extensions", "base"))
                .path("lives").path(0);
        if (live.isMissingNode()) return null;
        return new Weather(live.path("province").asText(), live.path("city").asText(),
                live.path("weather").asText(), live.path("temperature").asText(),
                live.path("winddirection").asText(), live.path("windpower").asText(),
                live.path("humidity").asText(), live.path("reporttime").asText());
    }

    public record Weather(String province, String city, String weather, String temperature,
                          String windDirection, String windPower, String humidity, String reportTime) {}
}
