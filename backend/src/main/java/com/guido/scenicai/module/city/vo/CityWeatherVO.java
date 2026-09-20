package com.guido.scenicai.module.city.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CityWeatherVO {
    private String weather;
    private String temperature;
    private String windDirection;
    private String windPower;
    private String humidity;
    private String reportTime;
    private String source;
}
