package com.guido.scenicai.module.city.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.city.entity.City;
import com.guido.scenicai.module.city.mapper.CityMapper;
import com.guido.scenicai.module.city.service.CityContextService;
import com.guido.scenicai.module.city.service.CityDiscoveryService;
import com.guido.scenicai.module.city.vo.CityContextVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tourist")
@RequiredArgsConstructor
public class TouristCityController {
    private final CityMapper cityMapper;
    private final CityContextService cityContextService;
    private final CityDiscoveryService cityDiscoveryService;

    @GetMapping("/cities")
    public Result<List<City>> cities() {
        return Result.ok(cityMapper.selectList(new LambdaQueryWrapper<City>()
                .eq(City::getStatus, 1).orderByAsc(City::getSortOrder).orderByAsc(City::getId)));
    }

    @GetMapping("/cities/{cityId}/context")
    public Result<CityContextVO> context(@PathVariable Long cityId) {
        City city = cityMapper.selectById(cityId);
        if (city == null || !Integer.valueOf(1).equals(city.getStatus())) {
            return Result.ok(cityDiscoveryService.discover(null, null));
        }
        return Result.ok(cityContextService.load(city.getCityCode(), city.getCityName(), cityId));
    }

    @GetMapping("/context/resolve")
    public Result<CityContextVO> resolve(@RequestParam(required = false) String cityCode,
                                         @RequestParam(required = false) String cityName) {
        return Result.ok(cityDiscoveryService.discover(cityCode, cityName));
    }
}
