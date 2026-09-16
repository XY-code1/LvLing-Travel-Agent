package com.guido.scenicai.module.city.service;

import com.guido.scenicai.module.city.entity.City;

import java.util.Optional;

public interface CityResolver {
    Optional<City> resolve(String cityCode, String cityName);
}
