package com.guido.scenicai.module.tourist.controller;

import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.tourist.dto.TouristLoginDTO;
import com.guido.scenicai.module.tourist.dto.TouristRegisterDTO;
import com.guido.scenicai.module.tourist.service.TouristAuthService;
import com.guido.scenicai.module.tourist.vo.RegisterVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tourist/auth")
@RequiredArgsConstructor
public class TouristAuthController {

    private final TouristAuthService touristAuthService;

    /**
     * 游客注册：POST /api/tourist/auth/register。
     */
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody TouristRegisterDTO dto) {
        Long touristId = touristAuthService.register(dto);
        return Result.ok(new RegisterVO(touristId));
    }

    /**
     * 游客登录：POST /api/tourist/auth/login。
     */
    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody TouristLoginDTO dto) {
        return Result.ok(touristAuthService.login(dto));
    }

    /**
     * 游客退出：POST /api/tourist/auth/logout。
     */
    @PostMapping("/logout")
    public Result<?> logout() {
        touristAuthService.logout();
        return Result.ok();
    }
}
