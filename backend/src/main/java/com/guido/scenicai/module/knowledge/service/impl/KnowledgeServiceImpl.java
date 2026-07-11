package com.guido.scenicai.module.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.common.result.PageResult;
import com.guido.scenicai.integration.embedding.EmbeddingClientRouter;
import com.guido.scenicai.module.knowledge.dto.KnowledgePageQueryDTO;
import com.guido.scenicai.module.knowledge.dto.KnowledgeStatusDTO;
import com.guido.scenicai.module.knowledge.dto.KnowledgeTestDTO;
import com.guido.scenicai.module.knowledge.entity.KbChunk;
import com.guido.scenicai.module.knowledge.entity.KbDocument;
import com.guido.scenicai.module.knowledge.entity.KbTestRecord;
import com.guido.scenicai.module.knowledge.mapper.KbChunkMapper;
import com.guido.scenicai.module.knowledge.mapper.KbDocumentMapper;
import com.guido.scenicai.module.knowledge.mapper.KbTestRecordMapper;
import com.guido.scenicai.module.knowledge.service.KnowledgeService;
import com.guido.scenicai.module.knowledge.service.LocalRagService;
import com.guido.scenicai.module.knowledge.vo.KnowledgeDocumentVO;
import com.guido.scenicai.module.knowledge.vo.KnowledgeSyncVO;
import com.guido.scenicai.module.knowledge.vo.KnowledgeTestVO;
import com.guido.scenicai.module.knowledge.vo.KnowledgeUploadVO;
import com.guido.scenicai.module.scenic.entity.Scenic;
import com.guido.scenicai.module.scenic.mapper.ScenicMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private static final int SYNC_PENDING  = 0;
    private static final int SYNC_RUNNING  = 1;
    private static final int SYNC_SUCCESS  = 2;
    private static final int SYNC_FAILED   = 3;

    /** 每个 chunk 的目标字符数（约 400 个汉字 / 500 个字符） */
    private static final int CHUNK_SIZE    = 500;
    /** 相邻 chunk 的重叠字符数（保留上下文连贯） */
    private static final int CHUNK_OVERLAP = 50;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "pdf", "word", "doc", "docx",
            "txt", "md", "excel", "xls", "xlsx",
            "image", "jpg", "jpeg", "png");

    private final KbDocumentMapper      kbDocumentMapper;
    private final KbChunkMapper         kbChunkMapper;
    private final KbTestRecordMapper    kbTestRecordMapper;
    private final ScenicMapper          scenicMapper;
    private final EmbeddingClientRouter embeddingClientRouter;
    private final LocalRagService       localRagService;
    private final ObjectMapper          objectMapper;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    // -------------------------------------------------------------------------
    // 分页查询
    // -------------------------------------------------------------------------
    @Override
    public PageResult<KnowledgeDocumentVO> page(KnowledgePageQueryDTO query) {
        LambdaQueryWrapper<KbDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getScenicId() != null, KbDocument::getScenicId, query.getScenicId());
        if (StringUtils.hasText(query.getScenicName())) {
            List<Long> scenicIds = scenicMapper.selectList(new LambdaQueryWrapper<Scenic>()
                            .like(Scenic::getName, query.getScenicName()))
                    .stream()
                    .map(Scenic::getId)
                    .toList();
            if (scenicIds.isEmpty()) {
                return PageResult.empty(query.getPageNum(), query.getPageSize());
            }
            wrapper.in(KbDocument::getScenicId, scenicIds);
        }
        wrapper.eq(query.getSyncStatus() != null, KbDocument::getParseStatus, query.getSyncStatus());
        wrapper.orderByDesc(KbDocument::getId);
        Page<KbDocument> page = kbDocumentMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        Map<Long, String> scenicNameMap = loadScenicNameMap(page.getRecords());
        List<KnowledgeDocumentVO> records = page.getRecords().stream()
                .map(document -> toVO(document, scenicNameMap))
                .toList();
        return PageResult.of(page.getTotal(), query.getPageNum(), query.getPageSize(), records);
    }

    // -------------------------------------------------------------------------
    // 上传文件（保存到本地磁盘，等待同步）
    // -------------------------------------------------------------------------
    @Override
    @Transactional
    public KnowledgeUploadVO upload(MultipartFile file, Long scenicId) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "上传文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        String fileType = detectFileType(originalName);
        validateFileType(fileType);
        String storedPath = storeFile(file, fileType);

        KbDocument document = new KbDocument();
        document.setScenicId(scenicId);
        document.setFileName(StringUtils.hasText(originalName) ? originalName : "knowledge." + fileType);
        document.setFileType(fileType);
        document.setFilePath(storedPath);
        document.setFileSize(file.getSize());
        document.setParseStatus(SYNC_PENDING);
        document.setStatus(1);
        kbDocumentMapper.insert(document);
        return new KnowledgeUploadVO(document.getId(), document.getParseStatus());
    }

    // -------------------------------------------------------------------------
    // 同步：本地切片 → 写入 kb_chunk
    // -------------------------------------------------------------------------
    @Override
    @Transactional
    public KnowledgeSyncVO sync(Long id) {
        KbDocument document = getRequired(id);
        document.setParseStatus(SYNC_RUNNING);
        kbDocumentMapper.updateById(document);
        try {
            String text = readAsText(document.getFilePath());
            List<String> chunks = chunkText(text);
            kbChunkMapper.delete(new LambdaQueryWrapper<KbChunk>()
                    .eq(KbChunk::getDocId, document.getId()));
            int index = 0;
            int embeddedCount = 0;
            String embedError = null;
            for (String chunkContent : chunks) {
                KbChunk chunk = new KbChunk();
                chunk.setDocId(document.getId());
                chunk.setScenicId(document.getScenicId());
                chunk.setChunkIndex(index++);
                chunk.setContent(chunkContent);
                chunk.setSourceName(document.getFileName());
                chunk.setTokenCount(chunkContent.length());
                chunk.setStatus(1);
                try {
                    if (tryEmbed(chunk, chunkContent)) {
                        embeddedCount++;
                    }
                } catch (Exception e) {
                    if (embedError == null) {
                        embedError = limit(e.getMessage(), 200);
                    }
                }
                kbChunkMapper.insert(chunk);
            }
            // 如果全未生成向量，整体状态标记为失败
            boolean allFailed = embeddedCount == 0 && !chunks.isEmpty();
            document.setParseStatus(allFailed ? SYNC_FAILED : SYNC_SUCCESS);
            document.setChunkCount(chunks.size());
            document.setEmbedStatus(resolveEmbedStatus(chunks.size(), embeddedCount));
            document.setFailMsg(buildSyncMsg(chunks.size(), embeddedCount, embedError));
            kbDocumentMapper.updateById(document);
            log.info("文档 [{}] 本地切分完成，共 {} 个分块", document.getFileName(), chunks.size());
            return new KnowledgeSyncVO(document.getParseStatus(), document.getFailMsg());
        } catch (Exception e) {
            document.setParseStatus(SYNC_FAILED);
            document.setFailMsg(limit(e.getMessage(), 500));
            kbDocumentMapper.updateById(document);
            log.error("文档 [{}] 切分失败：{}", document.getFileName(), e.getMessage());
            return new KnowledgeSyncVO(document.getParseStatus(), document.getFailMsg());
        }
    }

    // -------------------------------------------------------------------------
    // 删除：仅删本地文档记录和 kb_chunk
    // -------------------------------------------------------------------------
    @Override
    @Transactional
    public void remove(Long id) {
        KbDocument document = getRequired(id);
        kbChunkMapper.delete(new LambdaQueryWrapper<KbChunk>()
                .eq(KbChunk::getDocId, document.getId()));
        kbDocumentMapper.deleteById(id);
    }

    // -------------------------------------------------------------------------
    // 修改状态
    // -------------------------------------------------------------------------
    @Override
    @Transactional
    public void changeStatus(KnowledgeStatusDTO dto) {
        KbDocument document = getRequired(dto.getId());
        document.setStatus(dto.getStatus());
        kbDocumentMapper.updateById(document);
        KbChunk update = new KbChunk();
        update.setStatus(dto.getStatus());
        kbChunkMapper.update(update, new LambdaQueryWrapper<KbChunk>()
                .eq(KbChunk::getDocId, dto.getId()));
    }

    // -------------------------------------------------------------------------
    // 检索测试
    // -------------------------------------------------------------------------
    @Override
    @Transactional
    public KnowledgeTestVO test(KnowledgeTestDTO dto) {
        long start = System.nanoTime();
        LocalRagService.RagResult result = localRagService.retrieve(null, dto.getQuestion());
        KnowledgeTestVO vo = toTestVO(result, elapsedMs(start));
        saveTestRecord(dto.getQuestion(), vo);
        return vo;
    }

    // =========================================================================
    // 辅助方法
    // =========================================================================

    private KbDocument getRequired(Long id) {
        KbDocument document = kbDocumentMapper.selectById(id);
        if (document == null) {
            throw BizException.notFound("知识库文档不存在");
        }
        return document;
    }

    private KnowledgeTestVO toTestVO(LocalRagService.RagResult result, int costMs) {
        KnowledgeTestVO vo = new KnowledgeTestVO();
        vo.setHit(result.hit() ? 1 : 0);
        vo.setAnswer(result.hit() ? result.context() : "知识库未命中");
        vo.setSources(result.sources().stream()
                .map(s -> new com.guido.scenicai.module.knowledge.vo.KnowledgeTestSourceVO(
                        s.getDocName(), truncate(s.getSegment(), 300), s.getScore()))
                .toList());
        vo.setCostMs(costMs);
        return vo;
    }

    private void saveTestRecord(String question, KnowledgeTestVO vo) {
        KbTestRecord record = new KbTestRecord();
        record.setQuestion(question);
        record.setAnswer(vo.getAnswer());
        record.setHit(vo.getHit());
        record.setSources(toJson(vo.getSources()));
        record.setCostMs(vo.getCostMs());
        kbTestRecordMapper.insert(record);
    }

    private KnowledgeDocumentVO toVO(KbDocument document) {
        return toVO(document, Map.of());
    }

    private KnowledgeDocumentVO toVO(KbDocument document, Map<Long, String> scenicNameMap) {
        KnowledgeDocumentVO vo = new KnowledgeDocumentVO();
        vo.setId(document.getId());
        vo.setScenicId(document.getScenicId());
        vo.setScenicName(resolveScenicName(document.getScenicId(), scenicNameMap));
        vo.setFileName(document.getFileName());
        vo.setFileType(document.getFileType());
        vo.setFilePath(document.getFilePath());
        vo.setFileSize(document.getFileSize());
        vo.setChunkCount(document.getChunkCount());
        vo.setEmbedStatus(document.getEmbedStatus());
        vo.setSyncStatus(document.getParseStatus());
        vo.setSyncMsg(resolveSyncMsg(document));
        vo.setStatus(document.getStatus());
        vo.setCreateTime(document.getCreateTime());
        vo.setUpdateTime(document.getUpdateTime());
        return vo;
    }

    private Map<Long, String> loadScenicNameMap(List<KbDocument> documents) {
        List<Long> scenicIds = documents.stream()
                .map(KbDocument::getScenicId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (scenicIds.isEmpty()) {
            return Map.of();
        }
        return scenicMapper.selectBatchIds(scenicIds).stream()
                .collect(Collectors.toMap(Scenic::getId, Scenic::getName, (left, right) -> left));
    }

    private String resolveScenicName(Long scenicId, Map<Long, String> scenicNameMap) {
        if (scenicId == null) {
            return "全局知识库";
        }
        String scenicName = scenicNameMap.get(scenicId);
        if (scenicName != null) {
            return scenicName;
        }
        Scenic scenic = scenicMapper.selectById(scenicId);
        return scenic == null ? null : scenic.getName();
    }

    private String storeFile(MultipartFile file, String fileType) {
        try {
            Path dir = Path.of(uploadPath, "knowledge");
            Files.createDirectories(dir);
            String fileName = UUID.randomUUID() + "." + fileType;
            Path target = dir.resolve(fileName);
            Files.write(target, file.getBytes());
            return target.toString();
        } catch (Exception e) {
            throw new BizException(500, "知识库文件保存失败", e);
        }
    }

    private String readAsText(String filePath) {
        try {
            byte[] bytes = Files.readAllBytes(Path.of(filePath));
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BizException(500, "文件读取失败：" + e.getMessage());
        }
    }

    /**
     * 将文本切分为多个 chunk，按段落合并，超长段落再按字符拆分，相邻 chunk 保留重叠。
     */
    private List<String> chunkText(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        String[] paragraphs = text.split("\\n{2,}");
        List<String> result = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            // 超长段落自身按 CHUNK_SIZE 拆分
            if (trimmed.length() > CHUNK_SIZE) {
                // 先把当前 buffer 存起来
                if (buffer.length() > 0) {
                    result.add(buffer.toString().trim());
                    buffer = new StringBuilder();
                }
                for (int i = 0; i < trimmed.length(); i += CHUNK_SIZE - CHUNK_OVERLAP) {
                    int end = Math.min(i + CHUNK_SIZE, trimmed.length());
                    String seg = trimmed.substring(i, end).trim();
                    if (!seg.isEmpty()) {
                        result.add(seg);
                    }
                }
                continue;
            }
            // 加入 buffer 后会超出上限，先存 buffer 再重新开始
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
        return result.isEmpty() ? List.of(truncate(text, CHUNK_SIZE)) : result;
    }

    private boolean tryEmbed(KbChunk chunk, String content) {
        List<Float> vector = embeddingClientRouter.embed(content);
        if (vector != null && !vector.isEmpty()) {
            try {
                chunk.setEmbedding(objectMapper.writeValueAsString(vector));
            } catch (Exception e) {
                throw new BizException(500, "向量序列化失败", e);
            }
            chunk.setEmbedDim(vector.size());
            return true;
        }
        return false;
    }

    private Integer resolveEmbedStatus(int chunkCount, int embeddedCount) {
        if (chunkCount <= 0) {
            return SYNC_PENDING;
        }
        return embeddedCount == chunkCount ? SYNC_SUCCESS : SYNC_FAILED;
    }

    private String buildSyncMsg(int chunkCount, int embeddedCount, String embedError) {
        if (embeddedCount == chunkCount && chunkCount > 0) {
            return "切分完成，共 " + chunkCount + " 个分块，已生成 " + embeddedCount + " 条向量";
        }
        if (embeddedCount > 0) {
            return "切分完成，共 " + chunkCount + " 个分块，仅生成 " + embeddedCount + " 条向量" +
                    (embedError != null ? "，原因：" + embedError : "");
        }
        return "切分完成，共 " + chunkCount + " 个分块，未生成向量" +
                (embedError != null ? "，原因：" + embedError : "；请配置并启用本地向量模型后重新同步");
    }

    private String resolveSyncMsg(KbDocument document) {
        if (StringUtils.hasText(document.getFailMsg())) {
            return document.getFailMsg();
        }
        Integer status = document.getParseStatus();
        if (Integer.valueOf(SYNC_SUCCESS).equals(status)) {
            Integer chunkCount = document.getChunkCount();
            return "切分完成，共 " + (chunkCount == null ? 0 : chunkCount) + " 个分块";
        }
        if (Integer.valueOf(SYNC_RUNNING).equals(status)) {
            return "同步中";
        }
        if (Integer.valueOf(SYNC_FAILED).equals(status)) {
            return "同步失败";
        }
        return "待同步";
    }

    private String detectFileType(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "txt";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private void validateFileType(String fileType) {
        if (!ALLOWED_TYPES.contains(fileType)) {
            throw new BizException(1004, "文件类型不支持：" + fileType);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BizException(500, "JSON 序列化失败", e);
        }
    }

    private int elapsedMs(long start) {
        return Math.toIntExact((System.nanoTime() - start) / 1_000_000L);
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLen ? value.substring(0, maxLen) : value;
    }
}
