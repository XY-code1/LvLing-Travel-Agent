package com.guido.scenicai.module.admin.controller;

import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.admin.dto.AdminLoginDTO;
import com.guido.scenicai.module.admin.dto.AdminRegisterDTO;
import com.guido.scenicai.module.admin.dto.AdminResetPasswordDTO;
import com.guido.scenicai.module.admin.service.SysAdminService;
import com.guido.scenicai.module.admin.service.CaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final SysAdminService sysAdminService;
    private final CaptchaService captchaService;

    @GetMapping("/captcha")
    public Result<?> captcha() { return Result.ok(captchaService.create()); }

    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody AdminRegisterDTO dto) {
        sysAdminService.register(dto);
        return Result.ok();
    }

    @PostMapping("/forgot-password")
    public Result<?> resetPassword(@Valid @RequestBody AdminResetPasswordDTO dto) {
        sysAdminService.resetPassword(dto);
        return Result.ok();
    }

    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody AdminLoginDTO dto, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        return Result.ok(sysAdminService.login(dto, ip, userAgent));
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        sysAdminService.logout();
        return Result.ok();
    }

    @GetMapping("/info")
    public Result<?> info() {
        return Result.ok(sysAdminService.getCurrentAdminInfo());
    }
}
