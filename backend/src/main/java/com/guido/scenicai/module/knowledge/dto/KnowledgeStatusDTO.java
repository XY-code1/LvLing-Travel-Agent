package com.guido.scenicai.module.knowledge.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KnowledgeStatusDTO {

    @NotNull(message = "文档ID不能为空")
    private Long id;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
