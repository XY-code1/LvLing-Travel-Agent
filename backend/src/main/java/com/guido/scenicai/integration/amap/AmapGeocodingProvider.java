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
        return coordinate == null ? null : new Result(first.path("formatted_address").asText(),
                text(first.path("province")), text(first.path("city")), text(first.path("district")),
                first.path("adcode").asText(), coordinate);
    }

    public Result reverseGeocode(double longitude, double latitude, boolean wgs84) {
        AmapPoiProvider.Coordinate coordinate = new AmapPoiProvider.Coordinate(longitude, latitude);
        if (wgs84) {
            JsonNode converted = client.get("/v3/assistant/coordinate/convert", Map.of(
                    "locations", location(coordinate), "coordsys", "gps"));
            coordinate = AmapPoiProvider.Coordinate.parse(converted.path("locations").asText());
            if (coordinate == null) return null;
        }
        JsonNode regeo = client.get("/v3/geocode/regeo", Map.of(
                "location", location(coordinate), "extensions", "base", "radius", "1000"))
                .path("regeocode");
        JsonNode component = regeo.path("addressComponent");
        String province = text(component.path("province"));
        String city = text(component.path("city"));
        if (city.isBlank()) city = province;
        String adcode = component.path("adcode").asText();
        return adcode.isBlank() ? null : new Result(regeo.path("formatted_address").asText(),
                province, city, text(component.path("district")), adcode, coordinate);
    }

    private String location(AmapPoiProvider.Coordinate coordinate) {
        return coordinate.longitude() + "," + coordinate.latitude();
    }

    private String text(JsonNode value) {
        if (value.isArray()) return value.isEmpty() ? "" : value.path(0).asText();
        return value.asText();
    }

    public record Result(String formattedAddress, String province, String city, String district,
                         String adcode, AmapPoiProvider.Coordinate coordinates) {}
}
