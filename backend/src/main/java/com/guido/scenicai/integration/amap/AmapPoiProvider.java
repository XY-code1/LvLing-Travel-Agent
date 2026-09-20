package com.guido.scenicai.integration.amap;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AmapPoiProvider {
    private final AmapWebServiceClient client;

    public List<Poi> search(String keywords, String city) {
        JsonNode body = client.get("/v5/place/text", Map.of("keywords", keywords, "region", city,
                "city_limit", "true", "page_size", "10", "show_fields", "business"));
        List<Poi> result = new ArrayList<>();
        for (JsonNode node : body.path("pois")) {
            Coordinate coordinate = Coordinate.parse(node.path("location").asText());
            if (coordinate != null) {
                result.add(new Poi(node.path("id").asText(), node.path("name").asText(),
                        node.path("address").asText(), node.path("type").asText(),
                        integer(node.path("distance").asText()), coordinate));
            }
        }
        return result;
    }

    private Integer integer(String value) {
        try { return value == null || value.isBlank() ? null : Integer.valueOf(value); }
        catch (NumberFormatException ignored) { return null; }
    }

    public record Poi(String id, String name, String address, String type, Integer distanceMeters,
                      Coordinate coordinates) {
        public Poi(String id, String name, String address, Coordinate coordinates) {
            this(id, name, address, "", null, coordinates);
        }
    }

    public record Coordinate(double longitude, double latitude) {
        static Coordinate parse(String value) {
            if (value == null || !value.contains(",")) return null;
            String[] parts = value.split(",", 2);
            try {
                return new Coordinate(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }
}
