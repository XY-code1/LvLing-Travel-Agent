package com.guido.scenicai.module.city.provider;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guido.scenicai.module.city.entity.City;
import com.guido.scenicai.module.city.mapper.CityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LocalCityProvider implements CityProvider {
    private final CityMapper cityMapper;

    @Override
    public Optional<City> find(String cityCode, String cityName) {
        LambdaQueryWrapper<City> query = new LambdaQueryWrapper<City>()
                .eq(City::getStatus, 1)
                .last("LIMIT 1");
        if (StringUtils.hasText(cityCode)) {
            query.eq(City::getCityCode, cityCode.trim());
        } else if (StringUtils.hasText(cityName)) {
            query.eq(City::getCityName, cityName.trim());
        } else {
            query.orderByAsc(City::getSortOrder).orderByAsc(City::getId);
        }
        return Optional.ofNullable(cityMapper.selectOne(query));
    }
}
