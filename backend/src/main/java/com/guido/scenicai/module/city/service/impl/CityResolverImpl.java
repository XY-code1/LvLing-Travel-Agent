package com.guido.scenicai.module.city.service.impl;

import com.guido.scenicai.module.city.entity.City;
import com.guido.scenicai.module.city.provider.CityProvider;
import com.guido.scenicai.module.city.service.CityResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CityResolverImpl implements CityResolver {
    private final CityProvider cityProvider;

    @Override
    public Optional<City> resolve(String cityCode, String cityName) {
        return cityProvider.find(cityCode, cityName);
    }
}
