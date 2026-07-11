package com.guido.scenicai.integration.embedding;

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

/**
 * 本地向量化客户端（OpenAI-Compatible /v1/embeddings 协议）。
 *
 * <p>只负责 HTTP 调用，不自行加载配置；配置由 {@link EmbeddingClientRouter} 统一注入，
 * 便于批量向量化时复用同一份 AiConfigSnapshot，避免逐条查库。
 *
 * <p>base_url 指向本地或自建 embedding 服务（如 Xinference / Ollama / LocalAI 暴露的
 * bge / m3e 模型接口），从而实现"本地知识库向量化"，不依赖任何外部托管知识库平台。
 */
@Component
@RequiredArgsConstructor
public class OpenAiCompatibleEmbeddingClient {

    private final ObjectMapper objectMapper;

    public List<Float> embed(AiConfigSnapshot config, String text) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", config.getModelName());
            payload.put("input", text);
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(resolveEndpoint(config.getBaseUrl())))
                    .timeout(Duration.ofMillis(config.getTimeoutMs() == null ? 10000 : config.getTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
                builder.header("Authorization", "Bearer " + config.getApiKey());
            }
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String respSnippet = response.body() != null && response.body().length() > 200
                        ? response.body().substring(0, 200) : response.body();
                throw new BizException(ResultCode.AI_SERVICE_ERROR.getCode(),
                        "Embedding 调用失败，HTTP " + response.statusCode() + "：" + respSnippet);
            }
            JsonNode root = objectMapper.readTree(response.body());
            List<Float> vector = parseEmbedding(root);
            if (vector.isEmpty()) {
                throw new BizException(ResultCode.AI_SERVICE_ERROR.getCode(), "Embedding 返回空向量");
            }
            return vector;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            String causeMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new BizException(ResultCode.AI_SERVICE_TIMEOUT.getCode(), "Embedding 调用异常：" + causeMsg, e);
        }
    }

    /**
     * 从响应 JSON 中解析出向量数据，兼容两种格式：
     * <ul>
     *   <li>OpenAI 兼容：{"data": [{"embedding": [...]}]}</li>
     *   <li>Ollama 原生：  {"embeddings": [[...]]}</li>
     * </ul>
     */
    private List<Float> parseEmbedding(JsonNode root) {
        // 尝试 Ollama 原生格式：{"embeddings": [[...]]}
        JsonNode embeds = root.path("embeddings");
        if (embeds.isArray() && embeds.size() > 0) {
            JsonNode first = embeds.path(0);
            if (first.isArray() && first.size() > 0) {
                return toFloatList(first);
            }
        }
        // 尝试 OpenAI 兼容格式：{"data": [{"embedding": [...]}]}
        JsonNode data = root.path("data").path(0).path("embedding");
        if (data.isArray() && data.size() > 0) {
            return toFloatList(data);
        }
        return List.of();
    }

    private List<Float> toFloatList(JsonNode arr) {
        List<Float> result = new ArrayList<>(arr.size());
        for (JsonNode node : arr) {
            result.add((float) node.asDouble());
        }
        return result;
    }

    private String resolveEndpoint(String baseUrl) {
        String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        // URL 已包含 /embeddings 或 /embed，直接使用
        if (trimmed.endsWith("/embeddings") || trimmed.endsWith("/embed")) {
            return trimmed;
        }
        // 否则按 OpenAI 兼容协议追加 /embeddings
        return trimmed + "/embeddings";
    }
}
