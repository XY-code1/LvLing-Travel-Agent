package com.guido.scenicai.module.log.dto;

import com.guido.scenicai.common.base.BasePageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SysLog pagination query parameters.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysLogPageQueryDTO extends BasePageQuery {

    /** Filter by log type */
    private String logType;

    /** Filter by success: 1=success 0=failure */
    private Integer success;

    /** Filter by operator (fuzzy match) */
    private String operator;

    /** Start time (yyyy-MM-dd HH:mm:ss) */
    private String startTime;

    /** End time (yyyy-MM-dd HH:mm:ss) */
    private String endTime;
}
