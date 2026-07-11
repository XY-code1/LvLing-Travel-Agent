package com.guido.scenicai.module.knowledge.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeTestSourceVO {

    private String docName;
    private String segment;
    private Double score;
}
