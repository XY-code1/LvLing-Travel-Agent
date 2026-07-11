package com.guido.scenicai.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库分块+向量（本地向量库核心表）
 * embedding 以 JSON 数组字符串存储，检索时在应用层做余弦相似度
 */
@Data
@TableName("kb_chunk")
public class KbChunk {

    private Long id;
    private Long docId;
    private Long scenicId;
    /** 关联景点（用于视觉识别映射） */
    private Long spotId;
    /** 在文档内的顺序号 */
    private Integer chunkIndex;
    /** 分块正文（送入 LLM 的上下文） */
    private String content;
    /** 标题路径，如"核心景点特色详解>灵山大佛" */
    private String titlePath;
    private Integer tokenCount;
    /** 向量，JSON 数组字符串 */
    private String embedding;
    /** 向量维度 */
    private Integer embedDim;
    /** 生成该向量的模型名 */
    private String embedModel;
    /** 来源展示名（供答案溯源） */
    private String sourceName;
    private Integer status;
    private LocalDateTime createTime;
}
