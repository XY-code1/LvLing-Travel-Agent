package com.guido.scenicai.module.aiconfig.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guido.scenicai.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_service_config")
public class AiServiceConfig extends BaseEntity {

    private String serviceType;
    private String provider;
    private String protocol;
    private String baseUrl;
    private String apiKey;
    private String accessKeyId;
    private String accessKeySecret;
    private String appKey;
    private String region;
    private String modelName;
    private String datasetId;
    private String extraConfig;
    private Integer timeoutMs;
    private Integer retryCount;
    private Integer capabilityVerified;
    private LocalDateTime verifiedTime;
    private String verifyMsg;
    private Integer isDefault;
    private Integer enabled;
    private String remark;
}
