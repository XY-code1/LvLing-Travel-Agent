package com.guido.scenicai.integration.vlm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.common.result.ResultCode;
import com.guido.scenicai.integration.common.AiConfigSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiCompatibleVlmAdapter implements VlmClient {

    private static final String PROTOCOL = "OPENAI_COMPATIBLE";

    private final ObjectMapper objectMapper;

    @Override
    public String protocol() {
        return PROTOCOL;
    }

    @Override
    public VlmRecognizeResponse recognize(AiConfigSnapshot config, VlmRecognizeRequest request) {
        try {
            String body = objectMapper.writeValueAsString(buildBody(config, request));
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(resolveEndpoint(config.getBaseUrl())))
                    .timeout(Duration.ofMillis(config.getTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException(ResultCode.AI_SERVICE_ERROR.getCode(),
                        "OpenAI-Compatible 视觉调用失败：" + response.statusCode() + " " + limit(response.body(), 160));
            }
            JsonNode root = objectMapper.readTree(response.body());
            return new VlmRecognizeResponse(extractText(root), response.body());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ResultCode.AI_SERVICE_TIMEOUT.getCode(),
                    "OpenAI-Compatible 视觉调用异常：" + limit(rootCauseMessage(e), 160), e);
        }
    }

    private Map<String, Object> buildBody(AiConfigSnapshot config, VlmRecognizeRequest request) {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", prompt(request)));
        content.add(Map.of("type", "image_url", "image_url", Map.of("url", imageSource(request))));

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", content);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelName());
        body.put("messages", List.of(message));
        body.put("stream", false);
        body.put("max_tokens", request.getMaxTokens() == null ? 256 : request.getMaxTokens());
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        return body;
    }

    private String imageSource(VlmRecognizeRequest request) {
        if (StringUtils.hasText(request.getImageUrl())) {
            return request.getImageUrl().trim();
        }
        String imageBase64 = stripDataPrefix(request.getImageBase64());
        String mimeType = StringUtils.hasText(request.getMimeType()) ? request.getMimeType().trim() : "image/png";
        return "data:" + mimeType + ";base64," + imageBase64;
    }

    private String stripDataPrefix(String imageBase64) {
        if (!StringUtils.hasText(imageBase64)) {
            throw new BizException(400, "VISION 校验必须传测试图片");
        }
        String trimmed = imageBase64.trim();
        int commaIndex = trimmed.indexOf(',');
        return trimmed.startsWith("data:") && commaIndex >= 0 ? trimmed.substring(commaIndex + 1) : trimmed;
    }

    private String prompt(VlmRecognizeRequest request) {
        if (StringUtils.hasText(request.getPrompt())) {
            return request.getPrompt();
        }
        return "请识别图片中的主要内容，用一句中文回答。";
    }

    private String extractText(JsonNode root) {
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isTextual()) {
            return content.asText();
        }
        if (content.isArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonNode item : content) {
                if (item.has("text")) {
                    parts.add(item.path("text").asText());
                }
            }
            return String.join("", parts);
        }
        return "";
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return current.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private String resolveEndpoint(String baseUrl) {
        String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (trimmed.endsWith("/chat/completions")) {
            return trimmed;
        }
        return trimmed + "/chat/completions";
    }
}
