package com.guido.scenicai.module.scenic.controller;

import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.common.interceptor.OperationLog;
import com.guido.scenicai.module.scenic.dto.ScenicPageQueryDTO;
import com.guido.scenicai.module.scenic.dto.ScenicSaveDTO;
import com.guido.scenicai.module.scenic.dto.ScenicStatusDTO;
import com.guido.scenicai.module.scenic.dto.ScenicUpdateDTO;
import com.guido.scenicai.module.scenic.service.ScenicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/scenic")
@RequiredArgsConstructor
public class ScenicController {

    private final ScenicService scenicService;

    @GetMapping("/page")
    public Result<?> page(ScenicPageQueryDTO query) {
        return Result.ok(scenicService.pageQuery(query));
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(scenicService.getDetail(id));
    }

    @PostMapping
    @OperationLog("新增景区")
    public Result<?> create(@Valid @RequestBody ScenicSaveDTO dto) {
        return Result.ok(scenicService.create(dto));
    }

    @PutMapping
    @OperationLog("修改景区")
    public Result<?> modify(@Valid @RequestBody ScenicUpdateDTO dto) {
        return Result.ok(scenicService.modify(dto));
    }

    @DeleteMapping("/{id}")
    @OperationLog("移除景区")
    public Result<?> remove(@PathVariable Long id) {
        scenicService.remove(id);
        return Result.ok();
    }

    @PutMapping("/status")
    @OperationLog("景区启停")
    public Result<?> status(@Valid @RequestBody ScenicStatusDTO dto) {
        scenicService.changeStatus(dto);
        return Result.ok();
    }
}
