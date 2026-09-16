package com.guido.scenicai.module.city.service.impl;

import com.guido.scenicai.module.city.entity.City;
import com.guido.scenicai.module.city.provider.KnowledgeProvider;
import com.guido.scenicai.module.city.provider.POIProvider;
import com.guido.scenicai.module.city.provider.ServiceProvider;
import com.guido.scenicai.module.city.service.CityDiscoveryService;
import com.guido.scenicai.module.city.service.CityResolver;
import com.guido.scenicai.module.city.vo.CityContextVO;
import com.guido.scenicai.module.feature.entity.AdminFeatureItem;
import com.guido.scenicai.module.scenic.entity.Scenic;
import com.guido.scenicai.module.scenic.mapper.ScenicMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CityDiscoveryServiceImpl implements CityDiscoveryService {
    private final CityResolver cityResolver;
    private final POIProvider poiProvider;
    private final ServiceProvider serviceProvider;
    private final KnowledgeProvider knowledgeProvider;
    private final ScenicMapper scenicMapper;

    @Override
    public CityContextVO discover(String cityCode, String cityName) {
        City city = cityResolver.resolve(cityCode, cityName).orElseGet(() -> fallbackCity(cityCode, cityName));
        CityContextVO context = new CityContextVO();
        context.setCity(city);
        context.setDiscovered(city.getId() == null);
        context.setFallback(city.getId() == null);
        context.setMessage(city.getId() == null ? "本地暂无完整城市资料，已创建可扩展的发现上下文。" : null);
        if (city.getId() == null) {
            context.setScenicAreas(List.of());
            context.setPois(List.of());
            context.setServices(List.of());
            context.setAnnouncements(List.of());
            context.setKnowledgeSources(List.of());
            return context;
        }
        context.setScenicAreas(scenicMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Scenic>()
                .eq(Scenic::getCityId, city.getId()).eq(Scenic::getStatus, 1).orderByAsc(Scenic::getId)));
        context.setPois(poiProvider.list(city.getId()));
        context.setServices(serviceProvider.list(city.getId(), "FACILITY"));
        context.setAnnouncements(serviceProvider.list(city.getId(), "ANNOUNCEMENT"));
        context.setKnowledgeSources(knowledgeProvider.list(city.getId()));
        return context;
    }

    @Override
    public City fallbackCity(String cityCode, String cityName) {
        City city = new City();
        city.setCityCode(StringUtils.hasText(cityCode) ? cityCode.trim() : slug(cityName));
        city.setCityName(StringUtils.hasText(cityName) ? cityName.trim() : "待选择城市");
        city.setCountry("中国");
        city.setStatus(1);
        city.setSortOrder(9999);
        return city;
    }

    private String slug(String name) {
        String value = StringUtils.hasText(name) ? name.trim().toLowerCase(Locale.ROOT) : "unknown";
        return "discovered-" + Integer.toUnsignedString(value.hashCode());
    }
}
