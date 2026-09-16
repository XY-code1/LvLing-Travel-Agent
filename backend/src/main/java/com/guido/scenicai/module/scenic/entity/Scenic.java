package com.guido.scenicai.module.scenic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guido.scenicai.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scenic")
public class Scenic extends BaseEntity {

    private Long cityId;
    private String name;
    private String intro;
    private String address;
    private String openTime;
    private String ticketInfo;
    private String trafficInfo;
    private String servicePhone;
    private String notice;
    private String coverImage;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer status;
}
