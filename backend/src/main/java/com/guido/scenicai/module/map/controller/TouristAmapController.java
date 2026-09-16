package com.guido.scenicai.module.map.controller;

import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.integration.amap.AmapGeocodingProvider;
import com.guido.scenicai.integration.amap.AmapPoiProvider;
import com.guido.scenicai.integration.amap.AmapRouteProvider;
import com.guido.scenicai.integration.amap.AmapWeatherProvider;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tourist/amap")
@RequiredArgsConstructor
public class TouristAmapController {
    private final AmapPoiProvider poiProvider;
    private final AmapGeocodingProvider geocodingProvider;
    private final AmapRouteProvider routeProvider;
    private final AmapWeatherProvider weatherProvider;

    @GetMapping("/poi")
    public Result<?> poi(@RequestParam String keywords, @RequestParam String city) {
        return Result.ok(poiProvider.search(required(keywords, "keywords"), required(city, "city")));
    }

    @GetMapping("/geocode")
    public Result<?> geocode(@RequestParam String address, @RequestParam String city) {
        return Result.ok(geocodingProvider.geocode(required(address, "address"), required(city, "city")));
    }

    @GetMapping("/weather")
    public Result<?> weather(@RequestParam String city) {
        return Result.ok(weatherProvider.current(required(city, "city")));
    }

    @PostMapping("/route/walking")
    public Result<?> walking(@RequestBody RouteRequest request) {
        if (request.getPoints() == null || request.getPoints().size() < 2 || request.getPoints().size() > 16) {
            throw new BizException(400, "路线点数量必须为 2 至 16 个");
        }
        return Result.ok(routeProvider.walking(request.getPoints()));
    }

    private String required(String value, String name) {
        if (value == null || value.isBlank() || value.length() > 80) {
            throw new BizException(400, name + " 参数无效");
        }
        return value.trim();
    }

    @Data
    public static class RouteRequest {
        private List<AmapPoiProvider.Coordinate> points;
    }
}
