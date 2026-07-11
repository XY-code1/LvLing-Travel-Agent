package com.guido.scenicai.module.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guido.scenicai.integration.embedding.EmbeddingClientRouter;
import com.guido.scenicai.module.chat.vo.SourceVO;
import com.guido.scenicai.module.knowledge.entity.KbChunk;
import com.guido.scenicai.module.knowledge.mapper.KbChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 本地知识库 RAG 检索服务（完全本地，不依赖任何外部知识库平台）。
 *
 * <p>检索策略（对齐需求 E-02）：
 * <ol>
 *   <li>优先向量检索：把 query 经本地 embedding 服务向量化，与 kb_chunk 中存储的向量做余弦相似度，
 *       取 Top-K 且相似度 ≥ 阈值的分块；</li>
 *   <li>向量不可用（未配置 embedding 服务 / 分块无向量）时，降级为关键词匹配兜底，保证仍能召回；</li>
 *   <li>命中的分块拼成上下文交给 LLM，并回填引用来源 {@link SourceVO} 支撑溯源。</li>
 * </ol>
 *
 * <p>向量存储在 MySQL 的 kb_chunk.embedding（JSON 数组字符串），检索时在内存做余弦计算。
 * 示范景区数据量级下（数百分块）内存检索足够快；如后续数据量增大可平滑替换为向量数据库，
 * 检索接口不变。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalRagService {

    /** 关键词匹配最小连续字符数（低于此长度的公共子串不计分）。 */
    private static final int KEYWORD_MIN_GRAM = 3;
    /** 关键词匹配得分阈值：归一化后高于此值才算命中。 */
    private static final double KEYWORD_SCORE_THRESHOLD = 0.15;
    /** 相似度阈值：向量余弦相似度低于此值视为未命中。 */
    private static final double SIMILARITY_THRESHOLD = 0.5;
    /** 默认召回条数 Top-K。 */
    private static final int DEFAULT_TOP_K = 3;

    /** 每个 chunk 返回的最大字符数，超出截断。 */
    private static final int MAX_CHUNK_LENGTH = 1000;
    /** 总上下文字符数上限，超出截断。 */
    private static final int MAX_CONTEXT_LENGTH = 3000;

    private final KbChunkMapper kbChunkMapper;
    private final EmbeddingClientRouter embeddingClientRouter;
    private final ObjectMapper objectMapper;

    /**
     * 检索结果。
     *
     * @param hit      是否命中知识库
     * @param context  拼接后的上下文（供 LLM 使用），未命中为空串
     * @param sources  引用来源列表（供前端溯源），未命中为空列表
     */
    public record RagResult(boolean hit, String context, List<SourceVO> sources) {
        public static RagResult miss() {
            return new RagResult(false, "", List.of());
        }
    }

    /**
     * 检索指定景区的知识库。
     *
     * @param scenicId 景区 ID，null 表示不限景区
     * @param question 用户问题
     * @return 检索结果
     */
    public RagResult retrieve(Long scenicId, String question) {
        return retrieve(scenicId, question, DEFAULT_TOP_K);
    }

    public RagResult retrieve(Long scenicId, String question, int topK) {
        if (!StringUtils.hasText(question)) {
            return RagResult.miss();
        }
        List<KbChunk> candidates = loadCandidates(scenicId);
        if (candidates.isEmpty()) {
            return RagResult.miss();
        }

        // 1) 尝试向量检索
        List<Float> queryVector = tryEmbed(question);
        if (queryVector != null && !queryVector.isEmpty()) {
            RagResult vectorResult = vectorSearch(candidates, queryVector, topK);
            if (vectorResult.hit()) {
                return vectorResult;
            }
        }

        // 2) 兜底：关键词匹配
        return keywordSearch(candidates, question, topK);
    }

    private List<KbChunk> loadCandidates(Long scenicId) {
        LambdaQueryWrapper<KbChunk> wrapper = new LambdaQueryWrapper<KbChunk>()
                .eq(KbChunk::getStatus, 1);
        if (scenicId != null) {
            wrapper.and(w -> w.eq(KbChunk::getScenicId, scenicId).or().isNull(KbChunk::getScenicId));
        }
        return kbChunkMapper.selectList(wrapper);
    }

    private List<Float> tryEmbed(String text) {
        try {
            return embeddingClientRouter.embed(text);
        } catch (Exception e) {
            // embedding 服务未配置或调用失败：不抛出，降级到关键词兜底
            log.warn("本地向量化不可用，降级为关键词检索：{}", e.getMessage());
            return null;
        }
    }

    private RagResult vectorSearch(List<KbChunk> candidates, List<Float> queryVector, int topK) {
        List<Scored> scored = new ArrayList<>();
        for (KbChunk chunk : candidates) {
            List<Float> vector = parseVector(chunk.getEmbedding());
            if (vector == null || vector.size() != queryVector.size()) {
                continue;
            }
            double score = cosine(queryVector, vector);
            if (score >= SIMILARITY_THRESHOLD) {
                scored.add(new Scored(chunk, score));
            }
        }
        if (scored.isEmpty()) {
            return RagResult.miss();
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed());
        return toResult(scored, topK);
    }

    private RagResult keywordSearch(List<KbChunk> candidates, String question, int topK) {
        String normalized = question.toLowerCase();
        List<Scored> scored = new ArrayList<>();
        for (KbChunk chunk : candidates) {
            double score = keywordScore(normalized, chunk.getContent());
            if (score > 0) {
                scored.add(new Scored(chunk, score));
            }
        }
        if (scored.isEmpty()) {
            return RagResult.miss();
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed());
        return toResult(scored, topK);
    }

    private RagResult toResult(List<Scored> scored, int topK) {
        int limit = Math.min(topK, scored.size());
        StringBuilder context = new StringBuilder();
        List<SourceVO> sources = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            Scored item = scored.get(i);
            KbChunk chunk = item.chunk();
            String content = truncate(chunk.getContent(), MAX_CHUNK_LENGTH);
            if (context.length() + content.length() > MAX_CONTEXT_LENGTH) {
                content = truncate(content, MAX_CONTEXT_LENGTH - context.length());
            }
            context.append("【资料").append(i + 1).append("】")
                    .append(content).append("\n\n");
            sources.add(new SourceVO(
                    chunk.getSourceName(),
                    chunk.getTitlePath(),
                    truncate(content, 200),
                    round(item.score())));
            if (context.length() >= MAX_CONTEXT_LENGTH) {
                break;
            }
        }
        return new RagResult(true, context.toString().trim(), sources);
    }

    private double keywordScore(String normalizedQuestion, String content) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }
        String lower = content.toLowerCase();
        int qLen = normalizedQuestion.length();
        // 用最长公共子串长度衡量匹配度
        int maxCommon = 0;
        for (int i = 0; i < qLen; i++) {
            for (int j = i + KEYWORD_MIN_GRAM; j <= qLen && j - i <= 20; j++) {
                String sub = normalizedQuestion.substring(i, j);
                if (sub.isBlank()) continue;
                if (lower.contains(sub)) {
                    maxCommon = Math.max(maxCommon, sub.length());
                }
            }
        }
        if (maxCommon < KEYWORD_MIN_GRAM) {
            return 0;
        }
        double ratio = (double) maxCommon / qLen;
        return ratio >= KEYWORD_SCORE_THRESHOLD ? ratio : 0;
    }

    private List<Float> parseVector(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            Float[] arr = objectMapper.readValue(json, Float[].class);
            return List.of(arr);
        } catch (Exception e) {
            return null;
        }
    }

    private double cosine(List<Float> a, List<Float> b) {
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.size(); i++) {
            double x = a.get(i);
            double y = b.get(i);
            dot += x * y;
            normA += x * x;
            normB += y * y;
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private double round(double value) {
        return Math.round(value * 10000d) / 10000d;
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLen ? value.substring(0, maxLen) : value;
    }

    private record Scored(KbChunk chunk, double score) {
    }
}
