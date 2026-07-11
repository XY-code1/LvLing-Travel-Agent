package com.guido.scenicai.module.dashboard.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guido.scenicai.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("stat_daily")
public class StatDaily extends BaseEntity {

    private LocalDate statDate;
    private Integer serviceCount;
    private Integer qaCount;
    private Integer voiceCount;
    private Integer imageCount;
    private Integer avatarCount;
    private Integer avgCostMs;
    private BigDecimal kbHitRate;
    private Integer unansweredCount;
    private Integer negativeCount;
    private Integer errorCount;
}
