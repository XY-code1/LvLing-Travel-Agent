package com.guido.scenicai.module.dashboard.controller;

import com.guido.scenicai.common.interceptor.OperationLog;
import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.dashboard.dto.DemoSwitchDTO;
import com.guido.scenicai.module.dashboard.service.DashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public Result<?> overview() {
        return Result.ok(dashboardService.overview());
    }

    @PutMapping("/demo-switch")
    @OperationLog("切换演示数据")
    public Result<?> demoSwitch(@Valid @RequestBody DemoSwitchDTO dto) {
        dashboardService.updateDemoSwitch(dto);
        return Result.ok();
    }
}
