package com.guido.scenicai.integration.amap;

import com.fasterxml.jackson.databind.JsonNode;
import com.guido.scenicai.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AmapRouteProvider {
    private final AmapWebServiceClient client;

    public Route walking(List<AmapPoiProvider.Coordinate> points) {
        List<AmapPoiProvider.Coordinate> polyline = new ArrayList<>();
        int distance = 0;
        int duration = 0;
        for (int i = 1; i < points.size(); i++) {
            AmapPoiProvider.Coordinate origin = points.get(i - 1);
            AmapPoiProvider.Coordinate destination = points.get(i);
            JsonNode path = client.get("/v5/direction/walking", Map.of(
                    "origin", location(origin), "destination", location(destination), "show_fields", "cost"))
                    .path("route").path("paths").path(0);
            if (path.isMissingNode() || path.isNull() || path.isEmpty()) {
                throw new BizException(1002, "AMAP_ROUTE_NOT_FOUND");
            }
            distance += path.path("distance").asInt();
            duration += path.path("cost").path("duration").asInt();
            for (JsonNode step : path.path("steps")) {
                for (String location : step.path("polyline").asText().split(";")) {
                    AmapPoiProvider.Coordinate coordinate = AmapPoiProvider.Coordinate.parse(location);
                    if (coordinate != null && (polyline.isEmpty() || !polyline.get(polyline.size() - 1).equals(coordinate))) {
                        polyline.add(coordinate);
                    }
                }
            }
        }
        if (polyline.isEmpty()) {
            throw new BizException(1002, "AMAP_ROUTE_POLYLINE_EMPTY");
        }
        return new Route(distance, duration, polyline);
    }

    private String location(AmapPoiProvider.Coordinate point) {
        return point.longitude() + "," + point.latitude();
    }

    public record Route(int distanceMeters, int durationSeconds, List<AmapPoiProvider.Coordinate> polyline) {}
}
