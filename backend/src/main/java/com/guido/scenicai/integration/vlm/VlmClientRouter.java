package com.guido.scenicai.integration.vlm;

import com.guido.scenicai.common.constant.ServiceType;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.integration.common.AiConfigLoader;
import com.guido.scenicai.integration.common.AiConfigSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class VlmClientRouter {

    private final AiConfigLoader aiConfigLoader;
    private final List<VlmClient> clients;

    public VlmRecognizeResponse recognize(VlmRecognizeRequest request) {
        return recognize(aiConfigLoader.loadDefault(ServiceType.VISION), request);
    }

    public VlmRecognizeResponse recognize(AiConfigSnapshot config, VlmRecognizeRequest request) {
        return clients.stream()
                .filter(client -> client.protocol().equalsIgnoreCase(config.getProtocol()))
                .findFirst()
                .orElseThrow(() -> new BizException(400, "不支持的 VISION 协议：" + config.getProtocol()))
                .recognize(config, request);
    }
}
