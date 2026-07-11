package com.guido.scenicai.module.tourist.controller;

import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.tourist.dto.TouristProfileUpdateDTO;
import com.guido.scenicai.module.tourist.service.TouristUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tourist/user")
@RequiredArgsConstructor
public class TouristUserController {

    private final TouristUserService touristUserService;

    /**
     * 我的资料：GET /api/tourist/user/profile。
     */
    @GetMapping("/profile")
    public Result<?> getProfile() {
        return Result.ok(touristUserService.getProfile());
    }

    /**
     * 改资料/兴趣偏好：PUT /api/tourist/user/profile。
     */
    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody TouristProfileUpdateDTO dto) {
        return Result.ok(touristUserService.updateProfile(dto));
    }

    /**
     * 上传头像：POST /api/tourist/user/avatar。
     */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return Result.ok(touristUserService.uploadAvatar(file));
    }
}
