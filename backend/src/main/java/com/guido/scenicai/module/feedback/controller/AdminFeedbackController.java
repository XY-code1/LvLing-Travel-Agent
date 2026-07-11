package com.guido.scenicai.module.feedback.controller;

import com.guido.scenicai.common.interceptor.OperationLog;
import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.feedback.dto.AdminFeedbackPageQueryDTO;
import com.guido.scenicai.module.feedback.dto.FeedbackReplyDTO;
import com.guido.scenicai.module.feedback.service.AdminFeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/feedback")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final AdminFeedbackService adminFeedbackService;

    @GetMapping("/page")
    public Result<?> page(AdminFeedbackPageQueryDTO query) {
        return Result.ok(adminFeedbackService.page(query));
    }

    @PutMapping("/reply")
    @OperationLog("回复游客反馈")
    public Result<?> reply(@Valid @RequestBody FeedbackReplyDTO dto) {
        adminFeedbackService.reply(dto);
        return Result.ok();
    }
}
