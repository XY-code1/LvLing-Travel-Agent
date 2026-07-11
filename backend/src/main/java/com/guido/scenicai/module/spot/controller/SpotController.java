package com.guido.scenicai.module.spot.controller;

import com.guido.scenicai.common.interceptor.OperationLog;
import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.spot.dto.SpotPageQueryDTO;
import com.guido.scenicai.module.spot.dto.SpotSaveDTO;
import com.guido.scenicai.module.spot.dto.SpotStatusDTO;
import com.guido.scenicai.module.spot.dto.SpotUpdateDTO;
import com.guido.scenicai.module.spot.service.SpotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/spot")
@RequiredArgsConstructor
public class SpotController {

    private final SpotService spotService;

    @GetMapping("/page")
    public Result<?> page(SpotPageQueryDTO query) {
        return Result.ok(spotService.pageQuery(query));
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(spotService.getDetail(id));
    }

    @PostMapping
    @OperationLog("新增景点")
    public Result<?> create(@Valid @RequestBody SpotSaveDTO dto) {
        return Result.ok(spotService.create(dto));
    }

    @PutMapping
    @OperationLog("修改景点")
    public Result<?> modify(@Valid @RequestBody SpotUpdateDTO dto) {
        return Result.ok(spotService.modify(dto));
    }

    @DeleteMapping("/{id}")
    @OperationLog("移除景点")
    public Result<?> remove(@PathVariable Long id) {
        spotService.remove(id);
        return Result.ok();
    }

    @PutMapping("/status")
    @OperationLog("景点启停")
    public Result<?> status(@Valid @RequestBody SpotStatusDTO dto) {
        spotService.changeStatus(dto);
        return Result.ok();
    }
}
