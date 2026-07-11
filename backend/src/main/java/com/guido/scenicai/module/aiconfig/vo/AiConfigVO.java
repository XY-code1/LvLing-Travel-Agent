package com.guido.scenicai.module.aiconfig.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiConfigVO {

    private Long id;
    private String serviceType;
    private String provider;
    private String protocol;
    private String baseUrl;
    private String apiKey;
    private String accessKeyId;
    private Boolean hasSecret;
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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
