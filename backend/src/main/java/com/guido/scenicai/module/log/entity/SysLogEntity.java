package com.guido.scenicai.module.log.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guido.scenicai.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity mapping to sys_log table — unified log for LOGIN/OPERATION/CHAT/ASR/TTS/AVATAR/VISION/KB/EXCEPTION.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_log")
public class SysLogEntity extends BaseEntity {

    /** Log type: LOGIN/OPERATION/CHAT/ASR/TTS/AVATAR/VISION/KB/EXCEPTION */
    private String logType;

    /** Business description */
    private String bizDesc;

    /** Operator (admin username or tourist id) */
    private String operator;

    /** Third-party service provider (for AI calls) */
    private String serviceProvider;

    /** Request summary — must be desensitized, never contain full API keys */
    private String requestSummary;

    /** Response summary — must be desensitized */
    private String responseSummary;

    /** Duration in milliseconds */
    private Integer costMs;

    /** 1=success 0=failure */
    private Integer success;

    /** Error message if failed */
    private String errorMsg;

    /** Request IP */
    private String ip;
}
