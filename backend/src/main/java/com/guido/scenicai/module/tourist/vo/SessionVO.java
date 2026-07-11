package com.guido.scenicai.module.tourist.vo;

import lombok.Data;

@Data
public class SessionVO {

    private String sessionNo;
    private Long scenicId;
    private String title;
    private String lastTime;
    private Integer messageCount;
}
