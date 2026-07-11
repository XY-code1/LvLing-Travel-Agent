package com.guido.scenicai.module.tourist.controller;

import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.tourist.dto.SessionCreateDTO;
import com.guido.scenicai.module.tourist.service.ChatSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tourist/session")
@RequiredArgsConstructor
public class TouristSessionController {

    private final ChatSessionService chatSessionService;

    /**
     * 创建导览会话：POST /api/tourist/session/create。
     */
    @PostMapping("/create")
    public Result<?> createSession(@Valid @RequestBody SessionCreateDTO dto) {
        return Result.ok(chatSessionService.createSession(dto));
    }

    /**
     * 历史会话列表：GET /api/tourist/session/history。
     */
    @GetMapping("/history")
    public Result<?> getHistory() {
        return Result.ok(chatSessionService.getHistory());
    }

    /**
     * 会话消息拉取：GET /api/tourist/session/{sessionNo}/messages。
     */
    @GetMapping("/{sessionNo}/messages")
    public Result<?> getMessages(@PathVariable String sessionNo) {
        return Result.ok(chatSessionService.getMessages(sessionNo));
    }
}
