package com.guido.scenicai.integration.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.common.result.ResultCode;
import com.guido.scenicai.integration.common.AiConfigSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class OpenAiCompatibleAdapter implements LlmClient {

    private static final String PROTOCOL = "OPENAI_COMPATIBLE";

    private final ObjectMapper objectMapper;

    @Override
    public String protocol() {
        return PROTOCOL;
    }

    @Override
    public LlmChatResponse chat(AiConfigSnapshot config, LlmChatRequest request) {
        try {
            String body = objectMapper.writeValueAsString(buildBody(config, request, false));
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
                        "OpenAI-Compatible 调用失败：" + response.statusCode() + " " + limit(response.body(), 160));
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = extractText(root);
            return new LlmChatResponse(content, response.body());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ResultCode.AI_SERVICE_TIMEOUT.getCode(),
                    "OpenAI-Compatible 调用异常：" + limit(rootCauseMessage(e), 160), e);
        }
    }

    @Override
    public void stream(AiConfigSnapshot config, LlmChatRequest request, Consumer<String> onDelta) {
        try {
            String body = objectMapper.writeValueAsString(buildBody(config, request, true));
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(resolveEndpoint(config.getBaseUrl())))
                    .timeout(Duration.ofMillis(config.getTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<Stream<String>> response = HttpClient.newHttpClient()
                    .send(httpRequest, HttpResponse.BodyHandlers.ofLines());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException(ResultCode.AI_SERVICE_ERROR.getCode(), "OpenAI-Compatible 流式调用失败");
            }
            try (Stream<String> lines = response.body()) {
                lines.map(String::trim)
                        .filter(line -> line.startsWith("data:"))
                        .map(line -> line.substring(5).trim())
                        .takeWhile(data -> !"[DONE]".equals(data))
                        .map(this::extractStreamDelta)
                        .filter(delta -> delta != null && !delta.isBlank())
                        .forEach(onDelta);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ResultCode.AI_SERVICE_TIMEOUT.getCode(),
                    "OpenAI-Compatible 流式调用异常：" + limit(rootCauseMessage(e), 160), e);
        }
    }

    private Map<String, Object> buildBody(AiConfigSnapshot config, LlmChatRequest request, boolean stream) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            messages.add(Map.of("role", "system", "content", request.getSystemPrompt()));
        }
        messages.add(Map.of("role", "user", "content", request.getUserMessage()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelName());
        body.put("messages", messages);
        body.put("stream", stream);
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        return body;
    }

    private String extractStreamDelta(String data) {
        try {
            JsonNode root = objectMapper.readTree(data);
            return root.path("choices").path(0).path("delta").path("content").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractText(JsonNode root) {
        JsonNode choice = root.path("choices").path(0);
        String content = textFromNode(choice.path("message").path("content"));
        if (content != null) {
            return content;
        }
        content = choice.path("message").path("reasoning_content").asText(null);
        if (content != null && !content.isBlank()) {
            return content;
        }
        content = textFromNode(choice.path("text"));
        if (content != null) {
            return content;
        }
        content = textFromNode(root.path("output_text"));
        if (content != null) {
            return content;
        }
        return "";
    }

    private String textFromNode(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            String text = node.asText();
            return text.isBlank() ? null : text;
        }
        if (node.isArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonNode item : node) {
                String text = item.path("text").asText(null);
                if (text != null && !text.isBlank()) {
                    parts.add(text);
                }
            }
            return parts.isEmpty() ? null : String.join("", parts);
        }
        return null;
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
