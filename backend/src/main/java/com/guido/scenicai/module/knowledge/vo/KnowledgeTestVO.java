package com.guido.scenicai.module.knowledge.vo;

import lombok.Data;

import java.util.List;

@Data
public class KnowledgeTestVO {

    private String answer;
    private Integer hit;
    private List<KnowledgeTestSourceVO> sources;
    private Integer costMs;
}
