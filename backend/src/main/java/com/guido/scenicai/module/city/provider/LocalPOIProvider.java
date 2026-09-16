package com.guido.scenicai.module.city.provider;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guido.scenicai.module.spot.entity.Spot;
import com.guido.scenicai.module.spot.mapper.SpotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LocalPOIProvider implements POIProvider {
    private final SpotMapper spotMapper;

    @Override
    public List<Spot> list(Long cityId) {
        if (cityId == null) return List.of();
        return spotMapper.selectList(new LambdaQueryWrapper<Spot>()
                .eq(Spot::getCityId, cityId)
                .eq(Spot::getStatus, 1)
                .orderByDesc(Spot::getIsHot).orderByDesc(Spot::getId));
    }
}
