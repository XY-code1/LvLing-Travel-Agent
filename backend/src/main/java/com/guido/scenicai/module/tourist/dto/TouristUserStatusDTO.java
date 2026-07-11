package com.guido.scenicai.module.tourist.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TouristUserStatusDTO {

    @NotNull(message = "用户 ID 不能为空")
    private Long id;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
