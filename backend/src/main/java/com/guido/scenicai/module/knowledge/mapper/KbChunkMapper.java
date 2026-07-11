package com.guido.scenicai.module.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guido.scenicai.module.knowledge.entity.KbChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KbChunkMapper extends BaseMapper<KbChunk> {

    /** 按景区加载所有有效且已向量化的 chunk（用于余弦检索） */
    @Select("SELECT id, doc_id, scenic_id, spot_id, chunk_index, content, title_path, " +
            "embedding, embed_dim, embed_model, source_name " +
            "FROM kb_chunk WHERE status = 1 AND embedding IS NOT NULL " +
            "AND scenic_id = #{scenicId}")
    List<KbChunk> findEmbeddedByScenic(@Param("scenicId") Long scenicId);

    /** 加载全部有效且已向量化的 chunk（跨景区） */
    @Select("SELECT id, doc_id, scenic_id, spot_id, chunk_index, content, title_path, " +
            "embedding, embed_dim, embed_model, source_name " +
            "FROM kb_chunk WHERE status = 1 AND embedding IS NOT NULL")
    List<KbChunk> findAllEmbedded();

    /** 按文档ID删除所有分块（重建时用） */
    @Select("DELETE FROM kb_chunk WHERE doc_id = #{docId}")
    void deleteByDocId(@Param("docId") Long docId);
}
