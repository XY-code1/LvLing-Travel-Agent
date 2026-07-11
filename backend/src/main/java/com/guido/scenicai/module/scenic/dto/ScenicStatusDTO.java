package com.guido.scenicai.module.scenic.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScenicStatusDTO {

    @NotNull(message = "景区ID不能为空")
    private Long id;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
