package com.guido.scenicai.module.avatar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AvatarTestDTO {

    @NotNull(message = "形象ID不能为空")
    private Long id;

    @NotBlank(message = "测试文本不能为空")
    private String text;
}
