package com.guido.scenicai.module.aiconfig.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiConfigTestVO {

    private Boolean success;
    private Integer costMs;
    private String msg;
    private Integer capabilityVerified;
}
