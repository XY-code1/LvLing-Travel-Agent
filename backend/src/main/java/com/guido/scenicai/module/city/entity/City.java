package com.guido.scenicai.module.city.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guido.scenicai.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("city")
public class City extends BaseEntity {
    private String cityCode;
    private String cityName;
    private String province;
    private String country;
    private String description;
    private String slogan;
    private String coverImage;
    private String heroImages;
    private String themeConfig;
    private String weatherCode;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer status;
    private Integer sortOrder;
}
