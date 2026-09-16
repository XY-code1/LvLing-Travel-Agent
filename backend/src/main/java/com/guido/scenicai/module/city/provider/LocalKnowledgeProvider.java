package com.guido.scenicai.module.city.provider;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guido.scenicai.module.knowledge.entity.KbDocument;
import com.guido.scenicai.module.knowledge.mapper.KbDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LocalKnowledgeProvider implements KnowledgeProvider {
    private final KbDocumentMapper mapper;

    @Override
    public List<String> list(Long cityId) {
        if (cityId == null) return List.of();
        return mapper.selectList(new LambdaQueryWrapper<KbDocument>()
                        .eq(KbDocument::getCityId, cityId)
                        .eq(KbDocument::getStatus, 1)
                        .orderByDesc(KbDocument::getId))
                .stream().map(KbDocument::getFileName).toList();
    }
}
