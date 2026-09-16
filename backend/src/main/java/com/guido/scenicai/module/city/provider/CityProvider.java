package com.guido.scenicai.module.city.provider;

import com.guido.scenicai.module.city.entity.City;

import java.util.Optional;

public interface CityProvider {
    Optional<City> find(String cityCode, String cityName);
}
