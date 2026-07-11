package com.guido.scenicai.module.tourist.controller;

import com.guido.scenicai.common.interceptor.OperationLog;
import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.tourist.dto.AdminTouristUserPageQueryDTO;
import com.guido.scenicai.module.tourist.dto.TouristUserStatusDTO;
import com.guido.scenicai.module.tourist.service.AdminTouristUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class AdminTouristUserController {

    private final AdminTouristUserService adminTouristUserService;

    @GetMapping("/page")
    public Result<?> page(AdminTouristUserPageQueryDTO query) {
        return Result.ok(adminTouristUserService.page(query));
    }

    @PutMapping("/status")
    @OperationLog("游客用户启停")
    public Result<?> status(@Valid @RequestBody TouristUserStatusDTO dto) {
        adminTouristUserService.changeStatus(dto);
        return Result.ok();
    }
}
