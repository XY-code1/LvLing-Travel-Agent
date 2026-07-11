package com.guido.scenicai.module.scenic.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ScenicUpdateDTO extends ScenicSaveDTO {

    @NotNull(message = "景区ID不能为空")
    private Long id;
}
