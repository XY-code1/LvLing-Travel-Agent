package com.guido.scenicai.module.avatar.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AvatarSelectDTO {

    @NotNull(message = "形象ID不能为空")
    private Long avatarId;
}
