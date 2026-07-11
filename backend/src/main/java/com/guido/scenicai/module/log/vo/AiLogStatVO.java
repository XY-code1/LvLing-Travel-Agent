package com.guido.scenicai.module.log.vo;

import lombok.Data;

@Data
public class AiLogStatVO {

    private String logType;
    private String serviceProvider;
    private Long totalCount;
    private Long successCount;
    private Long failureCount;
    private Integer avgCostMs;
    private Integer maxCostMs;
}
