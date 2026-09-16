package com.guido.scenicai.module.city.service;

import com.guido.scenicai.module.city.vo.CityContextVO;

public interface CityContextService {
    CityContextVO load(String cityCode, String cityName, Long cityId);
}
