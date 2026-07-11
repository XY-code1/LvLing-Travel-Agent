package com.guido.scenicai.module.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TextChatDTO {

    @NotBlank(message = "会话编号不能为空")
    private String sessionNo;

    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "问题不能超过500字")
    private String question;

    private Long spotId;
}
