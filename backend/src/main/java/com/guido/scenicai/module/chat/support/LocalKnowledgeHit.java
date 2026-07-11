package com.guido.scenicai.module.chat.support;

import com.guido.scenicai.module.chat.vo.SourceVO;
import com.guido.scenicai.module.spot.entity.Spot;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LocalKnowledgeHit {

    private Spot spot;
    private String content;
    private SourceVO source;
}
