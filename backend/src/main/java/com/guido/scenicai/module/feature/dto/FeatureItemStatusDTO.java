package com.guido.scenicai.module.feature.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeatureItemStatusDTO {

    @NotNull(message = "配置 ID 不能为空")
    private Long id;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
