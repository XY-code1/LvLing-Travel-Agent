package com.guido.scenicai.module.city.vo;

import com.guido.scenicai.module.city.entity.City;
import com.guido.scenicai.module.scenic.entity.Scenic;
import com.guido.scenicai.module.feature.entity.AdminFeatureItem;
import lombok.Data;

import java.util.List;

@Data
public class CityContextVO {
    private City city;
    private List<Scenic> scenicAreas;
    private List<CityPoiVO> pois;
    private List<CityServiceVO> services;
    private List<AdminFeatureItem> announcements;
    private List<String> knowledgeSources;
    private Boolean discovered;
    private Boolean fallback;
    private String message;
    private String cityKey;
    private String adcode;
    private String source;
    private CityWeatherVO weather;
}
