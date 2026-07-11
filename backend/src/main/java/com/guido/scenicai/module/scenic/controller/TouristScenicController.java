package com.guido.scenicai.module.scenic.controller;

import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.spot.service.SpotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tourist/scenic")
@RequiredArgsConstructor
public class TouristScenicController {

    private final SpotService spotService;

    @GetMapping("/hot")
    public Result<?> hot() {
        return Result.ok(spotService.hotForTourist());
    }
}
