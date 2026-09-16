package com.guido.scenicai.module.route.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guido.scenicai.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("route")
public class Route extends BaseEntity {

    private Long cityId;
    private Long scenicId;
    private String name;
    private Integer type;
    private String intro;
    private Integer estimateMinutes;
    private String suitCrowd;
    private String interestTags;
    private String recommendReason;
    private String notice;
    private Integer status;
}
