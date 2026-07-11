package com.guido.scenicai.module.spot.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guido.scenicai.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("spot")
public class Spot extends BaseEntity {

    private Long scenicId;
    private String name;
    private String alias;
    private String intro;
    private String historyCulture;
    private String guideText;
    private String tags;
    private String images;
    private Integer stayMinutes;
    private String suitCrowd;
    private Integer isHot;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer status;
}
