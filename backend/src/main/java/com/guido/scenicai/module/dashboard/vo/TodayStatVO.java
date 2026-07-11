package com.guido.scenicai.module.dashboard.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TodayStatVO {

    private Integer serviceCount;
    private Integer qaCount;
    private Integer voiceCount;
    private Integer imageCount;
    private Integer avatarCount;
    private Integer avgCostMs;
    private BigDecimal kbHitRate;
    private Integer errorCount;
}
