package com.guido.scenicai.module.knowledge.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeUploadVO {

    private Long docId;
    private Integer syncStatus;
}
