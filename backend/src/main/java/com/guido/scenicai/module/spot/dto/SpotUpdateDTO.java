package com.guido.scenicai.module.spot.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SpotUpdateDTO extends SpotSaveDTO {

    @NotNull(message = "景点ID不能为空")
    private Long id;
}
