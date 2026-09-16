package com.guido.scenicai.module.city.service.impl;

import com.guido.scenicai.module.city.service.CityContextService;
import com.guido.scenicai.module.city.service.CityDiscoveryService;
import com.guido.scenicai.module.city.vo.CityContextVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CityContextServiceImpl implements CityContextService {
    private final CityDiscoveryService discoveryService;

    @Override
    public CityContextVO load(String cityCode, String cityName, Long cityId) {
        return discoveryService.discover(cityCode, cityName);
    }
}
