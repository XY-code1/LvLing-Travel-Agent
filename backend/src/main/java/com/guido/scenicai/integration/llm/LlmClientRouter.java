package com.guido.scenicai.integration.llm;

import com.guido.scenicai.common.constant.ServiceType;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.integration.common.AiConfigLoader;
import com.guido.scenicai.integration.common.AiConfigSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class LlmClientRouter {

    private final AiConfigLoader aiConfigLoader;
    private final List<LlmClient> clients;

    public LlmChatResponse chat(LlmChatRequest request) {
        AiConfigSnapshot config = aiConfigLoader.loadDefault(ServiceType.LLM);
        return client(config).chat(config, request);
    }

    public void stream(LlmChatRequest request, Consumer<String> onDelta) {
        AiConfigSnapshot config = aiConfigLoader.loadDefault(ServiceType.LLM);
        client(config).stream(config, request, onDelta);
    }

    private LlmClient client(AiConfigSnapshot config) {
        return clients.stream()
                .filter(client -> client.protocol().equalsIgnoreCase(config.getProtocol()))
                .findFirst()
                .orElseThrow(() -> new BizException(400, "不支持的 LLM 协议：" + config.getProtocol()))
                ;
    }
}
