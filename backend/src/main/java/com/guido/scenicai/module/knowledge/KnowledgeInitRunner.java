package com.guido.scenicai.module.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guido.scenicai.integration.embedding.EmbeddingClientRouter;
import com.guido.scenicai.module.knowledge.entity.KbChunk;
import com.guido.scenicai.module.knowledge.entity.KbDocument;
import com.guido.scenicai.module.knowledge.mapper.KbChunkMapper;
import com.guido.scenicai.module.knowledge.mapper.KbDocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 应用启动时自动导入 assets/knowledge_raw 目录下的知识文档。
 *
 * <p>每次启动检查该目录：若文档尚未在 kb_document 中存档，则切片写入 kb_chunk。
 * 已入库的文档跳过（幂等），不会重复切片。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeInitRunner implements ApplicationRunner {

    private static final int CHUNK_SIZE    = 500;
    private static final int CHUNK_OVERLAP = 50;

    private final KbDocumentMapper      kbDocumentMapper;
    private final KbChunkMapper         kbChunkMapper;
    private final EmbeddingClientRouter embeddingClientRouter;
    private final ObjectMapper          objectMapper;

    @Value("${knowledge.raw.path:./assets/knowledge_raw}")
    private String rawPath;

    @Override
    public void run(ApplicationArguments args) {
        Path dir = Path.of(rawPath);
        if (!Files.isDirectory(dir)) {
            log.warn("知识库原始文档目录不存在，跳过自动导入：{}", dir.toAbsolutePath());
            return;
        }
        try (Stream<Path> paths = Files.list(dir)) {
            paths.filter(p -> !Files.isDirectory(p))
                 .filter(p -> isSupportedType(p.getFileName().toString()))
                 .forEach(this::importFile);
        } catch (IOException e) {
            log.error("扫描知识库原始文档目录失败：{}", e.getMessage());
        }
    }

    private boolean isSupportedType(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".txt") || lower.endsWith(".md");
    }

    private void importFile(Path filePath) {
        String fileName = filePath.getFileName().toString();
        // 幂等：已有同名文档则跳过
        Long count = kbDocumentMapper.selectCount(
                new LambdaQueryWrapper<KbDocument>().eq(KbDocument::getFileName, fileName));
        if (count > 0) {
            log.debug("知识库文档已存在，跳过：{}", fileName);
            return;
        }
        log.info("自动导入知识库文档：{}", fileName);
        try {
            String text = Files.readString(filePath, StandardCharsets.UTF_8);
            // 写入文档记录
            KbDocument document = new KbDocument();
            document.setFileName(fileName);
            document.setFileType(fileType(fileName));
            document.setFilePath(filePath.toAbsolutePath().toString());
            document.setFileSize(Files.size(filePath));
            document.setParseStatus(2);
            document.setStatus(1);
            kbDocumentMapper.insert(document);
            // 切片写入 kb_chunk
            List<String> chunks = chunkText(text);
            int index = 0;
            int embeddedCount = 0;
            for (String content : chunks) {
                KbChunk chunk = new KbChunk();
                chunk.setDocId(document.getId());
                chunk.setChunkIndex(index++);
                chunk.setContent(content);
                chunk.setSourceName(fileName);
                chunk.setTokenCount(content.length());
                chunk.setStatus(1);
                if (tryEmbed(chunk, content)) {
                    embeddedCount++;
                }
                kbChunkMapper.insert(chunk);
            }
            document.setChunkCount(chunks.size());
            document.setEmbedStatus(embeddedCount == chunks.size() && !chunks.isEmpty() ? 2 : 3);
            document.setFailMsg(buildSyncMsg(chunks.size(), embeddedCount));
            kbDocumentMapper.updateById(document);
            log.info("文档 [{}] 导入完成，共 {} 个分块", fileName, chunks.size());
        } catch (Exception e) {
            log.error("文档 [{}] 自动导入失败：{}", fileName, e.getMessage());
        }
    }

    private List<String> chunkText(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        String[] paragraphs = text.split("\\n{2,}");
        List<String> result = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) continue;
            if (buffer.length() + trimmed.length() + 1 > CHUNK_SIZE && buffer.length() > 0) {
                result.add(buffer.toString().trim());
                String overlap = buffer.length() > CHUNK_OVERLAP
                        ? buffer.substring(buffer.length() - CHUNK_OVERLAP)
                        : buffer.toString();
                buffer = new StringBuilder(overlap).append("\n");
            }
            buffer.append(trimmed).append("\n");
        }
        if (buffer.length() > 0) {
            result.add(buffer.toString().trim());
        }
        return result.isEmpty() ? List.of(text.substring(0, Math.min(text.length(), CHUNK_SIZE))) : result;
    }

    private boolean tryEmbed(KbChunk chunk, String content) {
        try {
            List<Float> vector = embeddingClientRouter.embed(content);
            if (vector != null && !vector.isEmpty()) {
                chunk.setEmbedding(objectMapper.writeValueAsString(vector));
                chunk.setEmbedDim(vector.size());
                return true;
            }
        } catch (Exception e) {
            // embedding 服务未配置时跳过，关键词检索仍可用
            log.debug("Embedding 跳过（服务未配置）：{}", e.getMessage());
        }
        return false;
    }

    private String buildSyncMsg(int chunkCount, int embeddedCount) {
        if (embeddedCount == chunkCount && chunkCount > 0) {
            return "切分完成，共 " + chunkCount + " 个分块，已生成 " + embeddedCount + " 条向量";
        }
        if (embeddedCount > 0) {
            return "切分完成，共 " + chunkCount + " 个分块，仅生成 " + embeddedCount + " 条向量，请检查本地向量模型";
        }
        return "切分完成，共 " + chunkCount + " 个分块，未生成向量；请配置并启用本地向量模型后重新同步";
    }

    private String fileType(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1).toLowerCase() : "txt";
    }
}
