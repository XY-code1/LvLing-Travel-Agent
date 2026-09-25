package com.guido.scenicai.integration.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.common.result.ResultCode;
import com.guido.scenicai.integration.common.AiConfigSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiCompatibleAdapter implements LlmClient {

    private static final String PROTOCOL = "OPENAI_COMPATIBLE";

    /**
     * 推理型模型（如 deepseek-flash）会先输出 reasoning_content，它与正文共享 max_tokens。
     * 调用方常见的 400~600 预算会被推理过程耗尽，导致正文为空且 finish_reason=length，
     * 因此对过小的预算统一抬到该下限，保证推理之后仍能输出正文。
     */
    private static final int MIN_RESPONSE_TOKENS = 4096;

    /** 命中「推理耗尽预算」后的重试预算下限。 */
    private static final int RETRY_RESPONSE_TOKENS = 8192;

    private final ObjectMapper objectMapper;

    @Override
    public String protocol() {
        return PROTOCOL;
    }

    @Override
    public LlmChatResponse chat(AiConfigSnapshot config, LlmChatRequest request) {
        LlmChatResponse response = chatOnce(config, request);
        if (!answerTruncatedByReasoning(response.getRawResponse())) {
            return response;
        }
        log.warn("[LLM] 正文被推理内容截断（max_tokens={}），改用更大预算重试", request.getMaxTokens());
        return chatOnce(config, withRetryBudget(request));
    }

    private LlmChatResponse chatOnce(AiConfigSnapshot config, LlmChatRequest request) {
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
            return new LlmChatResponse(extractText(root), response.body());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ResultCode.AI_SERVICE_TIMEOUT.getCode(),
                    "OpenAI-Compatible 调用异常：" + limit(rootCauseMessage(e), 160), e);
        }
    }

    @Override
    public void stream(AiConfigSnapshot config, LlmChatRequest request, Consumer<String> onDelta) {
        if (streamOnce(config, request, onDelta)) {
            return;
        }
        log.warn("[LLM] 流式响应未产出正文（max_tokens={} 可能被推理内容耗尽），改用更大预算重试",
                request.getMaxTokens());
        if (!streamOnce(config, withRetryBudget(request), onDelta)) {
            throw new BizException(ResultCode.AI_SERVICE_ERROR.getCode(),
                    "文本大模型只返回了推理过程，没有产出回答正文；请调大 max_tokens 或改用非推理模型");
        }
    }

    /** 执行一次流式请求，返回是否已向 onDelta 推送过正文分片。 */
    private boolean streamOnce(AiConfigSnapshot config, LlmChatRequest request, Consumer<String> onDelta) {
        AtomicBoolean emitted = new AtomicBoolean(false);
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
                        .forEach(delta -> {
                            emitted.set(true);
                            onDelta.accept(delta);
                        });
            }
            return emitted.get();
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
        body.put("max_tokens", effectiveMaxTokens(request));
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        return body;
    }

    private int effectiveMaxTokens(LlmChatRequest request) {
        int requested = request.getMaxTokens() == null ? MIN_RESPONSE_TOKENS : request.getMaxTokens();
        return Math.max(requested, MIN_RESPONSE_TOKENS);
    }

    private LlmChatRequest withRetryBudget(LlmChatRequest request) {
        LlmChatRequest retry = new LlmChatRequest();
        retry.setSystemPrompt(request.getSystemPrompt());
        retry.setUserMessage(request.getUserMessage());
        retry.setTemperature(request.getTemperature());
        int requested = request.getMaxTokens() == null ? MIN_RESPONSE_TOKENS : request.getMaxTokens();
        retry.setMaxTokens(Math.max(requested * 4, RETRY_RESPONSE_TOKENS));
        return retry;
    }

    /**
     * 判断响应是否被推理过程耗尽预算：正文为空、只有 reasoning_content，且 finish_reason=length。
     * 这种情况必须重试，否则调用方会把推理过程误当成回答。
     */
    private boolean answerTruncatedByReasoning(String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) {
            return false;
        }
        try {
            JsonNode choice = objectMapper.readTree(rawResponse).path("choices").path(0);
            if (textFromNode(choice.path("message").path("content")) != null) {
                return false;
            }
            if (textFromNode(choice.path("message").path("reasoning_content")) == null) {
                return false;
            }
            return "length".equals(choice.path("finish_reason").asText(null));
        } catch (Exception e) {
            return false;
        }
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
