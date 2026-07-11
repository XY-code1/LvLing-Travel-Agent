package com.guido.scenicai.module.knowledge.controller;

import com.guido.scenicai.common.interceptor.OperationLog;
import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.knowledge.dto.KnowledgePageQueryDTO;
import com.guido.scenicai.module.knowledge.dto.KnowledgeStatusDTO;
import com.guido.scenicai.module.knowledge.dto.KnowledgeTestDTO;
import com.guido.scenicai.module.knowledge.service.KnowledgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @GetMapping("/page")
    public Result<?> page(KnowledgePageQueryDTO query) {
        return Result.ok(knowledgeService.page(query));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @OperationLog("上传知识库文档")
    public Result<?> upload(@RequestPart MultipartFile file,
                            @RequestParam(required = false) Long scenicId) {
        return Result.ok(knowledgeService.upload(file, scenicId));
    }

    @PostMapping("/sync/{id}")
    @OperationLog("同步知识库文档")
    public Result<?> sync(@PathVariable Long id) {
        return Result.ok(knowledgeService.sync(id));
    }

    @DeleteMapping("/{id}")
    @OperationLog("删除知识库文档")
    public Result<?> remove(@PathVariable Long id) {
        knowledgeService.remove(id);
        return Result.ok();
    }

    @PutMapping("/status")
    @OperationLog("知识库文档启停")
    public Result<?> status(@Valid @RequestBody KnowledgeStatusDTO dto) {
        knowledgeService.changeStatus(dto);
        return Result.ok();
    }

    @PostMapping("/test")
    public Result<?> test(@Valid @RequestBody KnowledgeTestDTO dto) {
        return Result.ok(knowledgeService.test(dto));
    }
}
