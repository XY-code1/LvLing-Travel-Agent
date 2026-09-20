package com.guido.scenicai.domain.route;

import com.guido.scenicai.integration.amap.AmapPoiProvider;

import java.util.List;

public record RouteResult(
        AmapPoiProvider.Coordinate origin,
        AmapPoiProvider.Coordinate destination,
        int distanceMeters,
        int durationSeconds,
        String routeMode,
        List<AmapPoiProvider.Coordinate> polyline,
        List<Step> steps,
        String provider) {

    public record Step(int distanceMeters, int durationSeconds, String instruction,
                       List<AmapPoiProvider.Coordinate> polyline) {
    }
}
