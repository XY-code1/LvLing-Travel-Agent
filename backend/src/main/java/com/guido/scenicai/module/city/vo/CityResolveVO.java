package com.guido.scenicai.module.city.vo;

import lombok.Data;

@Data
public class CityResolveVO {
    private Long cityId;
    private String cityCode;
    private String cityName;
    private String source;
    private Boolean discovered;
}
