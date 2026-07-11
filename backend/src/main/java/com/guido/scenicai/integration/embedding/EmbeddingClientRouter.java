package com.guido.scenicai.integration.embedding;

import com.guido.scenicai.common.constant.ServiceType;
import com.guido.scenicai.integration.common.AiConfigLoader;
import com.guido.scenicai.integration.common.AiConfigSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Embedding 路由：本地向量化服务统一入口。
 *
 * <p>当前仅支持 OpenAI-Compatible 协议（/v1/embeddings），可对接任意兼容该协议的
 * 本地部署 embedding 服务（如 bge/m3e 经 xinference/ollama/localai 暴露的接口），
 * 从而实现"本地知识库"要求——向量化不依赖任何第三方托管平台。
 *
 * <p>Router 负责从 ai_service_config 加载 EMBEDDING 默认配置；批量场景下复用同一份
 * 配置快照，避免逐条查库。
 */
@Component
@RequiredArgsConstructor
public class EmbeddingClientRouter {

    private final AiConfigLoader aiConfigLoader;
    private final OpenAiCompatibleEmbeddingClient openAiCompatibleEmbeddingClient;

    /** 加载当前启用的 EMBEDDING 配置快照（供批量向量化复用）。 */
    public AiConfigSnapshot loadConfig() {
        return aiConfigLoader.loadDefault(ServiceType.EMBEDDING);
    }

    /** 单条文本向量化（自动加载配置）。 */
    public List<Float> embed(String text) {
        return openAiCompatibleEmbeddingClient.embed(loadConfig(), text);
    }

    /** 用已加载的配置向量化单条文本（批量场景复用配置）。 */
    public List<Float> embed(AiConfigSnapshot config, String text) {
        return openAiCompatibleEmbeddingClient.embed(config, text);
    }

    /** 批量文本向量化，复用同一份配置。 */
    public List<List<Float>> embedBatch(List<String> texts) {
        AiConfigSnapshot config = loadConfig();
        List<List<Float>> result = new ArrayList<>(texts.size());
        for (String text : texts) {
            result.add(openAiCompatibleEmbeddingClient.embed(config, text));
        }
        return result;
    }
}
