package com.guido.scenicai.module.avatar.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvatarTestVO {

    private String streamUrl;
    private String audioUrl;
    private Integer costMs;
    private String msg;
}
