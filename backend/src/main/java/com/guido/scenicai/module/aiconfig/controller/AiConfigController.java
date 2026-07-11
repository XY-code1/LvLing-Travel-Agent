package com.guido.scenicai.module.aiconfig.controller;

import com.guido.scenicai.common.interceptor.OperationLog;
import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.aiconfig.dto.AiConfigEnableDTO;
import com.guido.scenicai.module.aiconfig.dto.AiConfigListQueryDTO;
import com.guido.scenicai.module.aiconfig.dto.AiConfigTestDTO;
import com.guido.scenicai.module.aiconfig.dto.AiConfigUpsertDTO;
import com.guido.scenicai.module.aiconfig.service.AiConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/ai-config")
@RequiredArgsConstructor
public class AiConfigController {

    private final AiConfigService aiConfigService;

    @GetMapping("/list")
    public Result<?> list(AiConfigListQueryDTO query) {
        return Result.ok(aiConfigService.list(query));
    }

    @PostMapping
    @OperationLog("新增AI服务配置")
    public Result<?> create(@Valid @RequestBody AiConfigUpsertDTO dto) {
        dto.setId(null);
        return Result.ok(aiConfigService.save(dto));
    }

    @PutMapping
    @OperationLog("修改AI服务配置")
    public Result<?> modify(@Valid @RequestBody AiConfigUpsertDTO dto) {
        return Result.ok(aiConfigService.save(dto));
    }

    @DeleteMapping("/{id}")
    @OperationLog("移除AI服务配置")
    public Result<?> remove(@PathVariable Long id) {
        aiConfigService.remove(id);
        return Result.ok();
    }

    @PutMapping("/enable")
    @OperationLog("启用默认AI服务配置")
    public Result<?> enable(@Valid @RequestBody AiConfigEnableDTO dto) {
        aiConfigService.enableDefault(dto.getId());
        return Result.ok();
    }

    @PostMapping("/test/{id}")
    @OperationLog("测试AI服务配置")
    public Result<?> test(@PathVariable Long id, @RequestBody(required = false) AiConfigTestDTO dto) {
        return Result.ok(aiConfigService.test(id, dto));
    }

    @PostMapping(value = "/test/{id}/vision", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @OperationLog("测试多模态模型配置")
    public Result<?> testVision(@PathVariable Long id,
                                @RequestPart("image") MultipartFile image,
                                @RequestParam(required = false) String prompt) {
        return Result.ok(aiConfigService.testVisionFile(id, image, prompt));
    }
}
