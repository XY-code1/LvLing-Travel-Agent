package com.guido.scenicai.module.chat.controller;

import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.chat.service.TouristRouteRecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tourist/route")
@RequiredArgsConstructor
public class TouristRouteController {

    private final TouristRouteRecommendService touristRouteRecommendService;

    @GetMapping("/recommend")
    public Result<?> recommend(@RequestParam Long scenicId,
                               @RequestParam(required = false) String interest,
                               @RequestParam(required = false) Integer routeType) {
        return Result.ok(touristRouteRecommendService.recommend(scenicId, interest, routeType));
    }
}
