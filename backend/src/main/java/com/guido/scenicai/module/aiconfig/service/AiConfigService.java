package com.guido.scenicai.module.aiconfig.service;

import com.guido.scenicai.module.aiconfig.dto.AiConfigListQueryDTO;
import com.guido.scenicai.module.aiconfig.dto.AiConfigTestDTO;
import com.guido.scenicai.module.aiconfig.dto.AiConfigUpsertDTO;
import com.guido.scenicai.module.aiconfig.vo.AiConfigTestVO;
import com.guido.scenicai.module.aiconfig.vo.AiConfigVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AiConfigService {

    List<AiConfigVO> list(AiConfigListQueryDTO query);

    AiConfigVO save(AiConfigUpsertDTO dto);

    void remove(Long id);

    void enableDefault(Long id);

    AiConfigTestVO test(Long id, AiConfigTestDTO dto);

    AiConfigTestVO testVisionFile(Long id, MultipartFile image, String prompt);
}
