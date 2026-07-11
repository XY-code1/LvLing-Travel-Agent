package com.guido.scenicai.integration.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guido.scenicai.integration.common.AiConfigSnapshot;

import java.util.logging.Logger;

public class LlmAdapterSmokeRunner {

    private static final Logger LOGGER = Logger.getLogger(LlmAdapterSmokeRunner.class.getName());

    public static void main(String[] args) {
        String apiKey = requiredEnv("LLM_API_KEY");
        String model = envOrDefault("LLM_MODEL", "deepseek-v4-flash");

        LlmChatRequest request = new LlmChatRequest();
        request.setUserMessage("Reply with exactly: OK");
        request.setMaxTokens(64);

        ObjectMapper objectMapper = new ObjectMapper();
        runOpenAi(objectMapper, apiKey, model, request);
        runAnthropic(objectMapper, apiKey, model, request);
    }

    private static void runOpenAi(ObjectMapper objectMapper, String apiKey,
                                  String model, LlmChatRequest request) {
        AiConfigSnapshot config = baseConfig("OPENAI_COMPATIBLE",
                envOrDefault("LLM_OPENAI_BASE_URL", "https://api.deepseek.com"),
                apiKey, model);
        LlmChatResponse response = new OpenAiCompatibleAdapter(objectMapper).chat(config, request);
        LOGGER.info(() -> "OPENAI_COMPATIBLE_CONTENT=" + response.getContent());
        LOGGER.info(() -> "OPENAI_COMPATIBLE_RAW=" + response.getRawResponse());
    }

    private static void runAnthropic(ObjectMapper objectMapper, String apiKey,
                                     String model, LlmChatRequest request) {
        AiConfigSnapshot config = baseConfig("ANTHROPIC_COMPATIBLE",
                envOrDefault("LLM_ANTHROPIC_BASE_URL", "https://api.deepseek.com/anthropic"),
                apiKey, model);
        LlmChatResponse response = new AnthropicCompatibleAdapter(objectMapper).chat(config, request);
        LOGGER.info(() -> "ANTHROPIC_COMPATIBLE_CONTENT=" + response.getContent());
        LOGGER.info(() -> "ANTHROPIC_COMPATIBLE_RAW=" + response.getRawResponse());
    }

    private static AiConfigSnapshot baseConfig(String protocol, String baseUrl,
                                               String apiKey, String model) {
        AiConfigSnapshot config = new AiConfigSnapshot();
        config.setProtocol(protocol);
        config.setBaseUrl(baseUrl);
        config.setApiKey(apiKey);
        config.setModelName(model);
        config.setTimeoutMs(30000);
        config.setRetryCount(0);
        return config;
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
