package com.guido.scenicai.module.log.controller;

import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.log.dto.SysLogPageQueryDTO;
import com.guido.scenicai.module.log.service.SysLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SysLog query controller — read-only log viewing for admin backend.
 */
@RestController
@RequestMapping("/api/admin/log")
@RequiredArgsConstructor
public class SysLogController {

    private final SysLogService sysLogService;

    @GetMapping("/page")
    public Result<?> pageQuery(SysLogPageQueryDTO query) {
        return Result.ok(sysLogService.pageQuery(query));
    }

    @GetMapping("/ai-stat")
    public Result<?> aiStat(SysLogPageQueryDTO query) {
        return Result.ok(sysLogService.aiStat(query));
    }
}
