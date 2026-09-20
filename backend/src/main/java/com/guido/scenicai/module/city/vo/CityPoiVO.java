package com.guido.scenicai.module.city.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CityPoiVO {
    private String id;
    private String name;
    private String address;
    private String type;
    private Integer distanceMeters;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String images;
    private String source;
}
