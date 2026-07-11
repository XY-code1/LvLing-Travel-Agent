package com.guido.scenicai.module.tourist.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SessionCreateDTO {

    @NotNull(message = "景区 ID 不能为空")
    private Long scenicId;

    private Long avatarId;
}
