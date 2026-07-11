package com.guido.scenicai.integration.llm;

import lombok.Data;

@Data
public class LlmChatRequest {

    private String systemPrompt;
    private String userMessage;
    private Double temperature;
    private Integer maxTokens;
}
