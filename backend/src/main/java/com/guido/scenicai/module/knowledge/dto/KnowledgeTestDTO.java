package com.guido.scenicai.module.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeTestDTO {

    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "问题不能超过500字")
    private String question;
}
