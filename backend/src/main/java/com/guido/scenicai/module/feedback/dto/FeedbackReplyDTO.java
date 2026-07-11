package com.guido.scenicai.module.feedback.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeedbackReplyDTO {

    @NotNull(message = "反馈 ID 不能为空")
    private Long id;

    private String replyContent;

    @NotNull(message = "处理状态不能为空")
    private Integer handleStatus;
}
