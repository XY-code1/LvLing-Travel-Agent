package com.guido.scenicai.integration.vlm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guido.scenicai.integration.common.AiConfigSnapshot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.logging.Logger;

public class VlmAdapterSmokeRunner {

    private static final Logger LOGGER = Logger.getLogger(VlmAdapterSmokeRunner.class.getName());

    private static final String ONE_PIXEL_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=";

    public static void main(String[] args) {
        String protocol = envOrDefault("VLM_PROTOCOL", "ANTHROPIC_COMPATIBLE");
        AiConfigSnapshot config = baseConfig(protocol);
        VlmRecognizeRequest request = new VlmRecognizeRequest();
        request.setImageBase64(loadImageBase64());
        request.setImageUrl(blankToNull(System.getenv("VLM_IMAGE_URL")));
        request.setMimeType(envOrDefault("VLM_IMAGE_MIME", "image/png"));
        request.setPrompt(envOrDefault("VLM_PROMPT", "Reply exactly OK if image input is accepted."));
        request.setMaxTokens(Integer.parseInt(envOrDefault("VLM_MAX_TOKENS", "8")));

        ObjectMapper objectMapper = new ObjectMapper();
        VlmClient client = "OPENAI_COMPATIBLE".equalsIgnoreCase(protocol)
                ? new OpenAiCompatibleVlmAdapter(objectMapper)
                : new AnthropicCompatibleVlmAdapter(objectMapper);
        VlmRecognizeResponse response = client.recognize(config, request);
        LOGGER.info(() -> "VLM_PROTOCOL=" + protocol);
        LOGGER.info(() -> "VLM_CONTENT=" + response.getContent());
        LOGGER.info(() -> "VLM_RAW=" + response.getRawResponse());
    }

    private static AiConfigSnapshot baseConfig(String protocol) {
        AiConfigSnapshot config = new AiConfigSnapshot();
        config.setProtocol(protocol);
        config.setBaseUrl(requiredEnv("VLM_BASE_URL"));
        config.setApiKey(requiredEnv("VLM_API_KEY"));
        config.setModelName(requiredEnv("VLM_MODEL"));
        config.setTimeoutMs(Integer.parseInt(envOrDefault("VLM_TIMEOUT_MS", "45000")));
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

    private static String loadImageBase64() {
        String imageFile = System.getenv("VLM_IMAGE_FILE");
        if (imageFile != null && !imageFile.isBlank()) {
            try {
                return Base64.getEncoder().encodeToString(Files.readAllBytes(Path.of(imageFile)));
            } catch (Exception e) {
                throw new IllegalStateException("VLM_IMAGE_FILE read failed", e);
            }
        }
        return envOrDefault("VLM_IMAGE_BASE64", ONE_PIXEL_PNG);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
