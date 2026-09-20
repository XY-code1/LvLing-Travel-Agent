package com.guido.scenicai.integration.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.common.result.ResultCode;
import com.guido.scenicai.common.security.CredentialCrypto;
import com.guido.scenicai.module.aiconfig.entity.AiServiceConfig;
import com.guido.scenicai.module.aiconfig.mapper.AiServiceConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AiConfigLoader {

    private final AiServiceConfigMapper aiServiceConfigMapper;
    private final CredentialCrypto credentialCrypto;

    @Value("${LLM_BASE_URL:}")
    private String llmBaseUrl;
    @Value("${LLM_MODEL:}")
    private String llmModel;
    @Value("${OPENAI_API_KEY:}")
    private String openAiApiKey;

    public AiConfigSnapshot loadDefault(String serviceType) {
        if ("LLM".equals(serviceType) && StringUtils.hasText(llmBaseUrl)
                && StringUtils.hasText(llmModel) && StringUtils.hasText(openAiApiKey)) {
            return environmentLlmConfig();
        }
        AiServiceConfig config = aiServiceConfigMapper.selectOne(new LambdaQueryWrapper<AiServiceConfig>()
                .eq(AiServiceConfig::getServiceType, serviceType)
                .eq(AiServiceConfig::getEnabled, 1)
                .eq(AiServiceConfig::getIsDefault, 1)
                .last("LIMIT 1"));
        if (config == null) {
            throw new BizException(ResultCode.AI_CONFIG_MISSING.getCode(), "AI 服务未配置或未启用：" + serviceType);
        }
        return toSnapshot(config);
    }

    private AiConfigSnapshot environmentLlmConfig() {
        AiConfigSnapshot snapshot = new AiConfigSnapshot();
        snapshot.setServiceType("LLM");
        snapshot.setProvider("OPENAI_COMPATIBLE");
        snapshot.setProtocol("OPENAI_COMPATIBLE");
        snapshot.setBaseUrl(llmBaseUrl);
        snapshot.setApiKey(openAiApiKey);
        snapshot.setModelName(llmModel);
        snapshot.setTimeoutMs(30000);
        snapshot.setRetryCount(1);
        return snapshot;
    }

    private AiConfigSnapshot toSnapshot(AiServiceConfig config) {
        AiConfigSnapshot snapshot = new AiConfigSnapshot();
        snapshot.setId(config.getId());
        snapshot.setServiceType(config.getServiceType());
        snapshot.setProvider(config.getProvider());
        snapshot.setProtocol(config.getProtocol());
        snapshot.setBaseUrl(config.getBaseUrl());
        snapshot.setApiKey(credentialCrypto.decrypt(config.getApiKey()));
        snapshot.setAccessKeyId(credentialCrypto.decrypt(config.getAccessKeyId()));
        snapshot.setAccessKeySecret(credentialCrypto.decrypt(config.getAccessKeySecret()));
        snapshot.setAppKey(config.getAppKey());
        snapshot.setRegion(config.getRegion());
        snapshot.setModelName(config.getModelName());
        snapshot.setDatasetId(config.getDatasetId());
        snapshot.setExtraConfig(config.getExtraConfig());
        snapshot.setTimeoutMs(config.getTimeoutMs());
        snapshot.setRetryCount(config.getRetryCount());
        return snapshot;
    }
}
