package com.guido.scenicai.integration.common;

import lombok.Data;

@Data
public class AiConfigSnapshot {

    private Long id;
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
}
