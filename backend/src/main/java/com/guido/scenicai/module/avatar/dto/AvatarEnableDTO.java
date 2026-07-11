package com.guido.scenicai.module.avatar.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AvatarEnableDTO {

    @NotNull(message = "形象ID不能为空")
    private Long id;

    @NotNull(message = "启停状态不能为空")
    private Integer enabled;
}
