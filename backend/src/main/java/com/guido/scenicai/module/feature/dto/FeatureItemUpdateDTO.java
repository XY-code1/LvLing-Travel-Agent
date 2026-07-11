package com.guido.scenicai.module.feature.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FeatureItemUpdateDTO extends FeatureItemSaveDTO {

    @NotNull(message = "配置 ID 不能为空")
    private Long id;
}
