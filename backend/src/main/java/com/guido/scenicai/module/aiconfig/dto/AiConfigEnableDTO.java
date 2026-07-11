package com.guido.scenicai.module.aiconfig.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiConfigEnableDTO {

    @NotNull(message = "配置ID不能为空")
    private Long id;
}
