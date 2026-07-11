package com.guido.scenicai.module.feature.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FeatureItemSaveDTO {

    @NotBlank(message = "模块类型不能为空")
    private String moduleType;

    private Long scenicId;
    private Long relatedId;

    @NotBlank(message = "名称不能为空")
    private String title;

    private String category;
    private String content;
    private String mediaUrl;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer sortOrder;
    private Integer status;
    private String remark;
}
