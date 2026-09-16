package com.guido.scenicai.module.city.provider;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guido.scenicai.module.feature.entity.AdminFeatureItem;
import com.guido.scenicai.module.feature.mapper.AdminFeatureItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LocalServiceProvider implements ServiceProvider {
    private final AdminFeatureItemMapper mapper;

    @Override
    public List<AdminFeatureItem> list(Long cityId, String moduleType) {
        if (cityId == null) return List.of();
        return mapper.selectList(new LambdaQueryWrapper<AdminFeatureItem>()
                .eq(AdminFeatureItem::getCityId, cityId)
                .eq(AdminFeatureItem::getModuleType, moduleType)
                .eq(AdminFeatureItem::getStatus, 1)
                .orderByAsc(AdminFeatureItem::getSortOrder));
    }
}
