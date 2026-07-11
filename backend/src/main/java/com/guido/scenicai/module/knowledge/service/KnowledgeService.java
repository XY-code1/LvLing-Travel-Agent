package com.guido.scenicai.module.knowledge.service;

import com.guido.scenicai.common.result.PageResult;
import com.guido.scenicai.module.knowledge.dto.KnowledgePageQueryDTO;
import com.guido.scenicai.module.knowledge.dto.KnowledgeStatusDTO;
import com.guido.scenicai.module.knowledge.dto.KnowledgeTestDTO;
import com.guido.scenicai.module.knowledge.vo.KnowledgeDocumentVO;
import com.guido.scenicai.module.knowledge.vo.KnowledgeSyncVO;
import com.guido.scenicai.module.knowledge.vo.KnowledgeTestVO;
import com.guido.scenicai.module.knowledge.vo.KnowledgeUploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface KnowledgeService {

    PageResult<KnowledgeDocumentVO> page(KnowledgePageQueryDTO query);

    KnowledgeUploadVO upload(MultipartFile file, Long scenicId);

    KnowledgeSyncVO sync(Long id);

    void remove(Long id);

    void changeStatus(KnowledgeStatusDTO dto);

    KnowledgeTestVO test(KnowledgeTestDTO dto);
}
