package com.guido.scenicai.module.chat.controller;

import com.guido.scenicai.common.interceptor.OperationLog;
import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.chat.dto.AdminChatPageQueryDTO;
import com.guido.scenicai.module.chat.dto.ChatMarkDTO;
import com.guido.scenicai.module.chat.service.AdminChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/chat")
@RequiredArgsConstructor
public class AdminChatController {

    private final AdminChatService adminChatService;

    @GetMapping("/page")
    public Result<?> page(AdminChatPageQueryDTO query) {
        return Result.ok(adminChatService.page(query));
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(adminChatService.detail(id));
    }

    @PutMapping("/mark")
    @OperationLog("标记待补充知识库")
    public Result<?> mark(@Valid @RequestBody ChatMarkDTO dto) {
        adminChatService.markSupplement(dto.getMessageId());
        return Result.ok();
    }
}
