package com.guido.scenicai.module.avatar.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TouristTtsDTO {
    @NotBlank(message = "TTS 文本不能为空")
    private String text;
    private String voiceId;
    private Integer speechRate;
    private Integer pitchRate;
}
