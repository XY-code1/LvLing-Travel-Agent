package com.guido.scenicai.module.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DemoSwitchDTO {

    @NotBlank(message = "开关键不能为空")
    private String switchKey;

    @NotNull(message = "开关状态不能为空")
    private Integer enabled;
}
