package com.guido.scenicai.integration.amap;

import com.fasterxml.jackson.databind.JsonNode;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.domain.route.RouteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AmapRouteProvider {
    private final AmapWebServiceClient client;

    public RouteResult walking(List<AmapPoiProvider.Coordinate> points) {
        List<AmapPoiProvider.Coordinate> polyline = new ArrayList<>();
        List<RouteResult.Step> steps = new ArrayList<>();
        int distance = 0;
        int duration = 0;
        for (int i = 1; i < points.size(); i++) {
            AmapPoiProvider.Coordinate origin = points.get(i - 1);
            AmapPoiProvider.Coordinate destination = points.get(i);
            JsonNode path = client.get("/v5/direction/walking", Map.of(
                    "origin", location(origin), "destination", location(destination),
                    "show_fields", "cost,polyline"))
                    .path("route").path("paths").path(0);
            if (path.isMissingNode() || path.isNull() || path.isEmpty()) {
                throw new BizException(1002, "AMAP_ROUTE_NOT_FOUND");
            }
            distance += path.path("distance").asInt();
            duration += path.path("cost").path("duration").asInt();
            for (JsonNode step : path.path("steps")) {
                List<AmapPoiProvider.Coordinate> stepPolyline = parsePolyline(step.path("polyline").asText());
                for (AmapPoiProvider.Coordinate coordinate : stepPolyline) {
                    if (polyline.isEmpty() || !polyline.get(polyline.size() - 1).equals(coordinate)) polyline.add(coordinate);
                }
                int stepDistance = step.hasNonNull("step_distance")
                        ? step.path("step_distance").asInt() : step.path("distance").asInt();
                steps.add(new RouteResult.Step(stepDistance,
                        step.path("cost").path("duration").asInt(), step.path("instruction").asText(), stepPolyline));
            }
        }
        if (polyline.isEmpty()) {
            throw new BizException(1002, "AMAP_ROUTE_POLYLINE_EMPTY");
        }
        return new RouteResult(points.get(0), points.get(points.size() - 1), distance, duration,
                "walking", List.copyOf(polyline), List.copyOf(steps), "amap");
    }

    private List<AmapPoiProvider.Coordinate> parsePolyline(String value) {
        List<AmapPoiProvider.Coordinate> result = new ArrayList<>();
        for (String location : value.split(";")) {
            AmapPoiProvider.Coordinate coordinate = AmapPoiProvider.Coordinate.parse(location);
            if (coordinate != null) result.add(coordinate);
        }
        return List.copyOf(result);
    }

    private String location(AmapPoiProvider.Coordinate point) {
        return point.longitude() + "," + point.latitude();
    }
}
