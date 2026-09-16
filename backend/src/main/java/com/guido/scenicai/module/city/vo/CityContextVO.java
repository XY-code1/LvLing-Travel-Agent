package com.guido.scenicai.module.city.vo;

import com.guido.scenicai.module.city.entity.City;
import com.guido.scenicai.module.scenic.entity.Scenic;
import com.guido.scenicai.module.spot.entity.Spot;
import com.guido.scenicai.module.feature.entity.AdminFeatureItem;
import lombok.Data;

import java.util.List;

@Data
public class CityContextVO {
    private City city;
    private List<Scenic> scenicAreas;
    private List<Spot> pois;
    private List<AdminFeatureItem> services;
    private List<AdminFeatureItem> announcements;
    private List<String> knowledgeSources;
    private Boolean discovered;
    private Boolean fallback;
    private String message;
}
