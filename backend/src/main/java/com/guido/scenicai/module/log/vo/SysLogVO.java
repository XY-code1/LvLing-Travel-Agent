package com.guido.scenicai.module.log.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * SysLog view object returned to frontend.
 */
@Data
public class SysLogVO {

    private Long id;
    private String logType;
    private String bizDesc;
    private String operator;
    private String serviceProvider;
    private String requestSummary;
    private String responseSummary;
    private Integer costMs;
    private Integer success;
    private String errorMsg;
    private String ip;
    private LocalDateTime createTime;
}
