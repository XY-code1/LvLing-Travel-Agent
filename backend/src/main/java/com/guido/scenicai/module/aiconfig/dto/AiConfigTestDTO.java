package com.guido.scenicai.module.aiconfig.dto;

import lombok.Data;

@Data
public class AiConfigTestDTO {

    private String imageBase64;
    private String imageUrl;
    private String mimeType;
    private String prompt;
    private String question;
}
