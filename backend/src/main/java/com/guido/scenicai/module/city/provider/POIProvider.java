package com.guido.scenicai.module.city.provider;

import com.guido.scenicai.module.spot.entity.Spot;

import java.util.List;

public interface POIProvider {
    List<Spot> list(Long cityId);
}
