package com.guido.scenicai.integration.llm;

import com.guido.scenicai.integration.common.AiConfigSnapshot;

import java.util.function.Consumer;

public interface LlmClient {

    String protocol();

    LlmChatResponse chat(AiConfigSnapshot config, LlmChatRequest request);

    default void stream(AiConfigSnapshot config, LlmChatRequest request, Consumer<String> onDelta) {
        LlmChatResponse response = chat(config, request);
        if (response != null && response.getContent() != null) {
            onDelta.accept(response.getContent());
        }
    }
}
