package com.guido.scenicai.module.feature.controller;

import com.guido.scenicai.common.interceptor.OperationLog;
import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.feature.dto.FeatureItemPageQueryDTO;
import com.guido.scenicai.module.feature.dto.FeatureItemSaveDTO;
import com.guido.scenicai.module.feature.dto.FeatureItemStatusDTO;
import com.guido.scenicai.module.feature.dto.FeatureItemUpdateDTO;
import com.guido.scenicai.module.feature.service.AdminFeatureItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/feature-item")
@RequiredArgsConstructor
public class AdminFeatureItemController {

    private final AdminFeatureItemService adminFeatureItemService;

    @GetMapping("/page")
    public Result<?> page(FeatureItemPageQueryDTO query) {
        return Result.ok(adminFeatureItemService.page(query));
    }

    @PostMapping
    @OperationLog("新增运营配置")
    public Result<?> create(@Valid @RequestBody FeatureItemSaveDTO dto) {
        return Result.ok(adminFeatureItemService.create(dto));
    }

    @PutMapping
    @OperationLog("修改运营配置")
    public Result<?> modify(@Valid @RequestBody FeatureItemUpdateDTO dto) {
        return Result.ok(adminFeatureItemService.modify(dto));
    }

    @DeleteMapping("/{id}")
    @OperationLog("删除运营配置")
    public Result<?> remove(@PathVariable Long id) {
        adminFeatureItemService.remove(id);
        return Result.ok();
    }

    @PutMapping("/status")
    @OperationLog("运营配置启停")
    public Result<?> status(@Valid @RequestBody FeatureItemStatusDTO dto) {
        adminFeatureItemService.changeStatus(dto);
        return Result.ok();
    }
}
