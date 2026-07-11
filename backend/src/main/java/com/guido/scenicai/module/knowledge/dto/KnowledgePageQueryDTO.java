package com.guido.scenicai.module.knowledge.dto;

import com.guido.scenicai.common.base.BasePageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgePageQueryDTO extends BasePageQuery {

    private Long scenicId;
    private String scenicName;
    private Integer syncStatus;
}
