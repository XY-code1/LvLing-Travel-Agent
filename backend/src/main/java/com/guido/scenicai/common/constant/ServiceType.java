package com.guido.scenicai.common.constant;

/**
 * AI service type constants for ai_service_config.service_type field.
 */
public final class ServiceType {

    private ServiceType() {
    }

    public static final String LLM = "LLM";
    public static final String VISION = "VISION";
    public static final String ASR = "ASR";
    public static final String TTS = "TTS";
    /**
     * 本地知识库向量化服务（Embedding）。
     * 仅用于本地 RAG 将文本转向量，不作为外部知识库平台配置入口。
     */
    public static final String EMBEDDING = "EMBEDDING";
    public static final String AVATAR = "AVATAR";
}
