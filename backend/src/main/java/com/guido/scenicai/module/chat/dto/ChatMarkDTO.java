package com.guido.scenicai.module.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatMarkDTO {

    @NotNull(message = "消息ID不能为空")
    private Long messageId;
}
