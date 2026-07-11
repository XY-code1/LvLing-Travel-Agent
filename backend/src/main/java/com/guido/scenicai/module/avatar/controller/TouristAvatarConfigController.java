package com.guido.scenicai.module.avatar.controller;

import com.guido.scenicai.common.config.StpTouristUtil;
import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.avatar.dto.AvatarSelectDTO;
import com.guido.scenicai.module.avatar.service.AvatarConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tourist/avatar")
@RequiredArgsConstructor
public class TouristAvatarConfigController {

    private final AvatarConfigService avatarConfigService;

    @GetMapping("/list")
    public Result<?> listEnabledAvatars() {
        return Result.ok(avatarConfigService.listEnabled());
    }

    @GetMapping("/current")
    public Result<?> getCurrentAvatar() {
        Long touristId = StpTouristUtil.stpLogic.getLoginIdAsLong();
        return Result.ok(avatarConfigService.getCurrentForTourist(touristId));
    }

    @GetMapping("/default")
    public Result<?> getDefaultAvatar() {
        Long touristId = StpTouristUtil.stpLogic.getLoginIdAsLong();
        return Result.ok(avatarConfigService.getCurrentForTourist(touristId));
    }

    @PutMapping("/current")
    public Result<?> selectAvatar(@Valid @RequestBody AvatarSelectDTO dto) {
        Long touristId = StpTouristUtil.stpLogic.getLoginIdAsLong();
        return Result.ok(avatarConfigService.selectForTourist(touristId, dto.getAvatarId()));
    }
}
