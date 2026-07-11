package com.guido.scenicai.module.spot.controller;

import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.spot.service.SpotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/tourist/spot")
@RequiredArgsConstructor
public class TouristSpotController {

    private final SpotService spotService;

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(spotService.getTouristDetail(id));
    }

    @GetMapping("/nearby")
    public Result<?> nearby(
            @RequestParam Long scenicId,
            @RequestParam BigDecimal longitude,
            @RequestParam BigDecimal latitude
    ) {
        return Result.ok(spotService.nearbyForTourist(scenicId, longitude, latitude));
    }
}
