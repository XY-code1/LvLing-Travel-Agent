package com.guido.scenicai.module.feature.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FeatureItemVO {

    private Long id;
    private String moduleType;
    private Long scenicId;
    private String scenicName;
    private Long relatedId;
    private String relatedName;
    private String title;
    private String category;
    private String content;
    private String mediaUrl;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer sortOrder;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
