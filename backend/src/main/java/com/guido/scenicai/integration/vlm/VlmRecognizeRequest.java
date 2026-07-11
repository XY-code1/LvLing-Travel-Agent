package com.guido.scenicai.integration.vlm;

import lombok.Data;

@Data
public class VlmRecognizeRequest {

    private String prompt;
    private String imageBase64;
    private String imageUrl;
    private String mimeType;
    private Integer maxTokens;
    private Double temperature;
}
