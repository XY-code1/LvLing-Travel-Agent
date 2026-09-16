package com.guido.scenicai.module.feature.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guido.scenicai.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_feature_item")
public class AdminFeatureItem extends BaseEntity {

    private Long cityId;
    private String moduleType;
    private Long scenicId;
    private Long relatedId;
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
