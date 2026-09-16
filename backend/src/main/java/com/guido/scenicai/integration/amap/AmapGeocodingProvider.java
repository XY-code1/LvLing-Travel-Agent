package com.guido.scenicai.integration.amap;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AmapGeocodingProvider {
    private final AmapWebServiceClient client;

    public Result geocode(String address, String city) {
        JsonNode first = client.get("/v3/geocode/geo", Map.of("address", address, "city", city))
                .path("geocodes").path(0);
        AmapPoiProvider.Coordinate coordinate = AmapPoiProvider.Coordinate.parse(first.path("location").asText());
        return coordinate == null ? null : new Result(first.path("formatted_address").asText(), coordinate);
    }

    public record Result(String formattedAddress, AmapPoiProvider.Coordinate coordinates) {}
}
