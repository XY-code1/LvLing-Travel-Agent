package com.guido.scenicai.module.city.service.impl;

import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.integration.amap.AmapGeocodingProvider;
import com.guido.scenicai.integration.amap.AmapPoiProvider;
import com.guido.scenicai.integration.amap.AmapWeatherProvider;
import com.guido.scenicai.module.city.entity.City;
import com.guido.scenicai.module.city.provider.KnowledgeProvider;
import com.guido.scenicai.module.city.provider.POIProvider;
import com.guido.scenicai.module.city.provider.ServiceProvider;
import com.guido.scenicai.module.city.service.CityDiscoveryService;
import com.guido.scenicai.module.city.service.CityResolver;
import com.guido.scenicai.module.city.vo.CityContextVO;
import com.guido.scenicai.module.city.vo.CityPoiVO;
import com.guido.scenicai.module.city.vo.CityServiceVO;
import com.guido.scenicai.module.city.vo.CityWeatherVO;
import com.guido.scenicai.module.feature.entity.AdminFeatureItem;
import com.guido.scenicai.module.scenic.entity.Scenic;
import com.guido.scenicai.module.scenic.mapper.ScenicMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.math.BigDecimal;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CityDiscoveryServiceImpl implements CityDiscoveryService {
    private final CityResolver cityResolver;
    private final POIProvider poiProvider;
    private final ServiceProvider serviceProvider;
    private final KnowledgeProvider knowledgeProvider;
    private final ScenicMapper scenicMapper;
    private final AmapGeocodingProvider amapGeocodingProvider;
    private final AmapPoiProvider amapPoiProvider;
    private final AmapWeatherProvider amapWeatherProvider;

    @Override
    public CityContextVO discover(String cityCode, String cityName) {
        City city = cityResolver.resolve(cityCode, cityName).orElse(null);
        boolean local = city != null && city.getId() != null;
        AmapGeocodingProvider.Result geocoding = null;
        if (!local) {
            String query = StringUtils.hasText(cityName) ? cityName.trim() : cityCode;
            if (!StringUtils.hasText(query)) {
                throw new BizException(404, "CITY_NOT_FOUND");
            }
            geocoding = amapGeocodingProvider.geocode(query, query);
            if (geocoding == null || !StringUtils.hasText(geocoding.adcode())) {
                throw new BizException(404, "CITY_NOT_FOUND: " + query);
            }
            city = discoveredCity(query, geocoding);
        } else {
            city.setCityKey("local:" + city.getId());
            city.setAdcode(city.getWeatherCode());
            city.setSource("local");
        }

        return buildContext(city, local);
    }

    @Override
    public CityContextVO discoverByLocation(double longitude, double latitude, boolean wgs84) {
        AmapGeocodingProvider.Result result = amapGeocodingProvider.reverseGeocode(longitude, latitude, wgs84);
        if (result == null || !StringUtils.hasText(result.adcode())) {
            throw new BizException(404, "LOCATION_CITY_NOT_FOUND");
        }
        City localCity = cityResolver.resolve(result.adcode(), normalizedCityName(result)).orElse(null);
        return buildContext(localCity != null ? localCity : discoveredCity(normalizedCityName(result), result),
                localCity != null && localCity.getId() != null);
    }

    private String normalizedCityName(AmapGeocodingProvider.Result result) {
        String name = StringUtils.hasText(result.city()) ? result.city() : result.province();
        return name.replaceFirst("市$", "");
    }

    private CityContextVO buildContext(City city, boolean local) {
        if (local) {
            city.setCityKey("local:" + city.getId());
            city.setAdcode(StringUtils.hasText(city.getWeatherCode()) ? city.getWeatherCode() : city.getCityCode());
            city.setSource("local");
        }
        CityContextVO context = new CityContextVO();
        context.setCity(city);
        context.setCityKey(city.getCityKey());
        context.setAdcode(city.getAdcode());
        context.setSource(city.getSource());
        context.setDiscovered(!local);
        context.setFallback(false);
        context.setMessage(local ? null : "已通过高德建立动态城市上下文。");
        context.setScenicAreas(local ? scenicMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Scenic>()
                .eq(Scenic::getCityId, city.getId()).eq(Scenic::getStatus, 1).orderByAsc(Scenic::getId)) : List.of());

        // Context resolution is intentionally lightweight. Remote POI data is loaded by PoiTool on demand.
        List<CityPoiVO> localPois = local ? poiProvider.list(city.getId()).stream().map(this::localPoi).toList() : List.of();
        context.setPois(localPois);

        List<CityServiceVO> localServices = local ? serviceProvider.list(city.getId(), "FACILITY").stream()
                .map(this::localService).toList() : List.of();
        // Remote service facilities are loaded lazily by ServiceTool when a task explicitly requires them.
        context.setServices(localServices);
        context.setAnnouncements(local ? serviceProvider.list(city.getId(), "ANNOUNCEMENT") : List.of());
        context.setKnowledgeSources(local ? knowledgeProvider.list(city.getId()) : List.of());
        // WeatherTool owns remote weather calls; do not fan them out during Context resolve.
        context.setWeather(null);
        return context;
    }

    private City discoveredCity(String requestedName, AmapGeocodingProvider.Result result) {
        City city = fallbackCity(result.adcode(), requestedName);
        String resolvedName = StringUtils.hasText(result.city()) ? result.city()
                : StringUtils.hasText(result.province()) ? result.province() : requestedName;
        city.setCityName(resolvedName.replaceFirst("市$", ""));
        city.setProvince(result.province());
        city.setCityCode(result.adcode());
        city.setCityKey("amap:" + result.adcode());
        city.setAdcode(result.adcode());
        city.setSource("discovered");
        city.setLongitude(BigDecimal.valueOf(result.coordinates().longitude()));
        city.setLatitude(BigDecimal.valueOf(result.coordinates().latitude()));
        return city;
    }

    private CityPoiVO localPoi(com.guido.scenicai.module.spot.entity.Spot spot) {
        return CityPoiVO.builder().id(String.valueOf(spot.getId())).name(spot.getName())
                .address(spot.getIntro()).type(spot.getTags()).longitude(spot.getLongitude())
                .latitude(spot.getLatitude()).images(spot.getImages()).source("local").build();
    }

    private CityPoiVO amapPoi(AmapPoiProvider.Poi poi, String fallbackType) {
        return CityPoiVO.builder().id(poi.id()).name(poi.name()).address(poi.address())
                .type(StringUtils.hasText(poi.type()) ? poi.type() : fallbackType)
                .distanceMeters(poi.distanceMeters())
                .longitude(BigDecimal.valueOf(poi.coordinates().longitude()))
                .latitude(BigDecimal.valueOf(poi.coordinates().latitude())).source("amap").build();
    }

    private CityServiceVO localService(AdminFeatureItem item) {
        return CityServiceVO.builder().id(String.valueOf(item.getId())).name(item.getTitle())
                .category(item.getCategory()).address(item.getContent()).longitude(item.getLongitude())
                .latitude(item.getLatitude()).source("local").build();
    }

    private List<CityServiceVO> amapServices(String cityName) {
        List<CityServiceVO> services = new ArrayList<>();
        addServices(services, cityName, "公共厕所", "卫生间");
        addServices(services, cityName, "餐厅", "餐厅");
        addServices(services, cityName, "停车场", "停车场");
        addServices(services, cityName, "医院", "医院/医疗");
        addServices(services, cityName, "游客服务中心", "游客服务中心");
        addServices(services, cityName, "充电站", "充电站");
        return services;
    }

    private void addServices(List<CityServiceVO> target, String city, String keyword, String category) {
        amapPoiProvider.search(keyword, city).stream().limit(5).map(poi -> CityServiceVO.builder()
                .id(poi.id()).name(poi.name()).category(category).address(poi.address())
                .longitude(BigDecimal.valueOf(poi.coordinates().longitude()))
                .latitude(BigDecimal.valueOf(poi.coordinates().latitude())).source("amap").build())
                .forEach(target::add);
    }

    private CityWeatherVO weather(String cityName) {
        AmapWeatherProvider.Weather value = amapWeatherProvider.current(cityName);
        if (value == null) return null;
        return CityWeatherVO.builder().weather(value.weather()).temperature(value.temperature())
                .windDirection(value.windDirection()).windPower(value.windPower())
                .humidity(value.humidity()).reportTime(value.reportTime()).source("amap").build();
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
