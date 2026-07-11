package com.guido.scenicai.integration.embedding;

import com.guido.scenicai.integration.common.AiConfigSnapshot;

import java.util.List;

/**
 * Embedding 客户端接口（本地向量化）。
 *
 * <p>实现：OpenAI-Compatible 协议（/v1/embeddings），可对接任意兼容该协议的
 * 本地/自建 embedding 服务。配置由 {@link EmbeddingClientRouter} 统一加载后传入，
 * 客户端本身不查库，便于批量向量化时复用同一份配置。
 */
public interface EmbeddingClient {

    /** 协议标识（如 OPENAI_COMPATIBLE）。 */
    String protocol();

    /**
     * 将文本转为向量。
     *
     * @param config 已解密的服务配置快照（service_type=EMBEDDING）
     * @param text   输入文本
     * @return 向量（float 列表）
     */
    List<Float> embed(AiConfigSnapshot config, String text);
}
