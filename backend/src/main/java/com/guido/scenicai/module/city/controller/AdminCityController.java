package com.guido.scenicai.module.city.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.module.city.entity.City;
import com.guido.scenicai.module.city.mapper.CityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/city")
@RequiredArgsConstructor
public class AdminCityController {
    private final CityMapper cityMapper;

    @GetMapping("/page")
    public Result<List<City>> page(@RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<City> query = new LambdaQueryWrapper<City>()
                .orderByAsc(City::getSortOrder).orderByAsc(City::getId);
        if (StringUtils.hasText(keyword)) {
            query.and(w -> w.like(City::getCityName, keyword).or().like(City::getCityCode, keyword));
        }
        return Result.ok(cityMapper.selectList(query));
    }

    @PostMapping
    public Result<City> create(@RequestBody City city) {
        city.setId(null);
        if (city.getCountry() == null) city.setCountry("中国");
        if (city.getStatus() == null) city.setStatus(1);
        cityMapper.insert(city);
        return Result.ok(city);
    }

    @PutMapping
    public Result<City> update(@RequestBody City city) {
        cityMapper.updateById(city);
        return Result.ok(cityMapper.selectById(city.getId()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> disable(@PathVariable Long id) {
        City city = cityMapper.selectById(id);
        if (city != null) {
            city.setStatus(0);
            cityMapper.updateById(city);
        }
        return Result.ok();
    }
}
