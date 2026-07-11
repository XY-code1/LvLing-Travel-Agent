package com.guido.scenicai.module.route.controller;

import com.guido.scenicai.common.interceptor.OperationLog;
import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.route.dto.RoutePageQueryDTO;
import com.guido.scenicai.module.route.dto.RouteSaveDTO;
import com.guido.scenicai.module.route.dto.RouteSpotsDTO;
import com.guido.scenicai.module.route.dto.RouteStatusDTO;
import com.guido.scenicai.module.route.dto.RouteUpdateDTO;
import com.guido.scenicai.module.route.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/route")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @GetMapping("/page")
    public Result<?> page(RoutePageQueryDTO query) {
        return Result.ok(routeService.pageQuery(query));
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(routeService.getDetail(id));
    }

    @PostMapping
    @OperationLog("新增路线")
    public Result<?> create(@Valid @RequestBody RouteSaveDTO dto) {
        return Result.ok(routeService.create(dto));
    }

    @PutMapping
    @OperationLog("修改路线")
    public Result<?> modify(@Valid @RequestBody RouteUpdateDTO dto) {
        return Result.ok(routeService.modify(dto));
    }

    @DeleteMapping("/{id}")
    @OperationLog("移除路线")
    public Result<?> remove(@PathVariable Long id) {
        routeService.remove(id);
        return Result.ok();
    }

    @PutMapping("/status")
    @OperationLog("路线启停")
    public Result<?> status(@Valid @RequestBody RouteStatusDTO dto) {
        routeService.changeStatus(dto);
        return Result.ok();
    }

    @PutMapping("/spots")
    @OperationLog("配置路线景点顺序")
    public Result<?> spots(@Valid @RequestBody RouteSpotsDTO dto) {
        routeService.configureSpots(dto);
        return Result.ok();
    }
}
