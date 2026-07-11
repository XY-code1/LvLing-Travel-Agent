package com.guido.scenicai.module.route.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RouteStatusDTO {

    @NotNull(message = "路线ID不能为空")
    private Long id;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
