package com.guido.scenicai.integration.llm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guido.scenicai.common.constant.ServiceType;
import com.guido.scenicai.module.aiconfig.entity.AiServiceConfig;
import com.guido.scenicai.module.aiconfig.mapper.AiServiceConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmRuntimeStatus {
    private final AiServiceConfigMapper configMapper;

    @Value("${LLM_BASE_URL:${OPENAI_BASE_URL:}}")
    private String llmBaseUrl;
    @Value("${LLM_MODEL:${OPENAI_MODEL:}}")
    private String llmModel;
    @Value("${OPENAI_API_KEY:${DEEPSEEK_API_KEY:}}")
    private String openAiApiKey;

    public Status current() {
        if (StringUtils.hasText(llmBaseUrl) && StringUtils.hasText(llmModel)
                && StringUtils.hasText(openAiApiKey)) {
            return new Status(true, "OPENAI_COMPATIBLE", llmModel, true, true);
        }
        AiServiceConfig config = configMapper.selectOne(new LambdaQueryWrapper<AiServiceConfig>()
                .eq(AiServiceConfig::getServiceType, ServiceType.LLM)
                .eq(AiServiceConfig::getEnabled, 1)
                .eq(AiServiceConfig::getIsDefault, 1)
                .last("LIMIT 1"));
        if (config == null) return new Status(false, null, null, false, false);
        return new Status(true, config.getProvider(), config.getModelName(),
                StringUtils.hasText(config.getBaseUrl()), StringUtils.hasText(config.getApiKey()));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logConfiguration() {
        Status status = current();
        log.info("[LLM] provider={} model={} baseUrlConfigured={} apiKeyConfigured={}",
                status.provider(), status.model(), status.baseUrlConfigured(), status.apiKeyConfigured());
    }

    public record Status(boolean live, String provider, String model,
                         boolean baseUrlConfigured, boolean apiKeyConfigured) {}
}
