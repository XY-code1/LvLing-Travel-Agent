package com.guido.scenicai.module.spot.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SpotStatusDTO {

    @NotNull(message = "景点ID不能为空")
    private Long id;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
