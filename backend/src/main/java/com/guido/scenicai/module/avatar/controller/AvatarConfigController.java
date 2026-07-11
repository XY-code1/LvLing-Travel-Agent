package com.guido.scenicai.module.avatar.controller;

import com.guido.scenicai.common.interceptor.OperationLog;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.avatar.dto.AvatarEnableDTO;
import com.guido.scenicai.module.avatar.dto.AvatarSaveDTO;
import com.guido.scenicai.module.avatar.dto.AvatarTestDTO;
import com.guido.scenicai.module.avatar.service.AvatarConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/avatar")
@RequiredArgsConstructor
public class AvatarConfigController {

    private final AvatarConfigService avatarConfigService;

    @GetMapping("/list")
    public Result<?> list() {
        return Result.ok(avatarConfigService.list());
    }

    @PostMapping
    @OperationLog("新增数字人形象")
    public Result<?> create(@Valid @RequestBody AvatarSaveDTO dto) {
        throw new BizException(400, "数字人形象库固定，请通过启用/停用控制游客端可选范围");
    }

    @PutMapping
    @OperationLog("修改数字人形象")
    public Result<?> modify(@Valid @RequestBody AvatarSaveDTO dto) {
        throw new BizException(400, "数字人形象库固定，请通过启用/停用控制游客端可选范围");
    }

    @PutMapping("/enable")
    @OperationLog("启用或停用数字人形象")
    public Result<?> enable(@Valid @RequestBody AvatarEnableDTO dto) {
        avatarConfigService.updateEnabled(dto.getId(), dto.getEnabled());
        return Result.ok();
    }

    @PostMapping("/test")
    @OperationLog("测试数字人播报")
    public Result<?> test(@Valid @RequestBody AvatarTestDTO dto) {
        return Result.ok(avatarConfigService.test(dto));
    }
}
