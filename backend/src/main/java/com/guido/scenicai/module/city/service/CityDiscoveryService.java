package com.guido.scenicai.module.city.service;

import com.guido.scenicai.module.city.entity.City;
import com.guido.scenicai.module.city.vo.CityContextVO;

public interface CityDiscoveryService {
    CityContextVO discover(String cityCode, String cityName);

    City fallbackCity(String cityCode, String cityName);
}
