package com.guido.scenicai.integration.llm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmChatResponse {

    private String content;
    private String rawResponse;
}
