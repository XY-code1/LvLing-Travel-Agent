package com.guido.scenicai.module.aiconfig.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiConfigUpsertDTO {

    private Long id;

    @NotBlank(message = "服务类型不能为空")
    private String serviceType;

    @NotBlank(message = "服务商不能为空")
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
    private Integer enabled;
    private String remark;
}
