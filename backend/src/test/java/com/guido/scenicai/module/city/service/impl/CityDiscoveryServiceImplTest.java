package com.guido.scenicai.module.city.service.impl;

import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.integration.amap.AmapGeocodingProvider;
import com.guido.scenicai.integration.amap.AmapPoiProvider;
import com.guido.scenicai.integration.amap.AmapWeatherProvider;
import com.guido.scenicai.module.city.provider.KnowledgeProvider;
import com.guido.scenicai.module.city.provider.POIProvider;
import com.guido.scenicai.module.city.provider.ServiceProvider;
import com.guido.scenicai.module.city.service.CityResolver;
import com.guido.scenicai.module.city.vo.CityContextVO;
import com.guido.scenicai.module.scenic.mapper.ScenicMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CityDiscoveryServiceImplTest {
    @Mock private CityResolver cityResolver;
    @Mock private POIProvider poiProvider;
    @Mock private ServiceProvider serviceProvider;
    @Mock private KnowledgeProvider knowledgeProvider;
    @Mock private ScenicMapper scenicMapper;
    @Mock private AmapGeocodingProvider geocodingProvider;
    @Mock private AmapPoiProvider poiProviderAmap;
    @Mock private AmapWeatherProvider weatherProvider;

    private CityDiscoveryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CityDiscoveryServiceImpl(cityResolver, poiProvider, serviceProvider, knowledgeProvider,
                scenicMapper, geocodingProvider, poiProviderAmap, weatherProvider);
    }

    @Test
    void discoversCityWithoutDatabaseId() {
        when(cityResolver.resolve(null, "成都")).thenReturn(Optional.empty());
        when(geocodingProvider.geocode("成都", "成都")).thenReturn(new AmapGeocodingProvider.Result(
                "四川省成都市", "四川省", "成都市", "", "510100",
                new AmapPoiProvider.Coordinate(104.066541, 30.572269)));
        when(poiProviderAmap.search(anyString(), org.mockito.ArgumentMatchers.eq("成都"))).thenAnswer(invocation -> {
            if ("旅游景点".equals(invocation.getArgument(0))) return List.of(new AmapPoiProvider.Poi(
                    "poi-1", "成都大熊猫繁育研究基地", "熊猫大道", "风景名胜", null,
                    new AmapPoiProvider.Coordinate(104.145, 30.734)));
            return List.of();
        });
        when(weatherProvider.current("成都")).thenReturn(new AmapWeatherProvider.Weather(
                "四川", "成都", "多云", "25", "南", "≤3", "60", "2026-09-18 00:00:00"));

        CityContextVO context = service.discover(null, "成都");

        assertNull(context.getCity().getId());
        assertEquals("成都", context.getCity().getCityName());
        assertEquals("amap:510100", context.getCityKey());
        assertEquals("discovered", context.getSource());
        assertTrue(context.getPois().isEmpty());
        assertNull(context.getWeather());
        assertTrue(context.getDiscovered());
    }

    @Test
    void rejectsUnknownCityInsteadOfCreatingFallbackData() {
        when(cityResolver.resolve(null, "不存在的城市名称")).thenReturn(Optional.empty());
        when(geocodingProvider.geocode("不存在的城市名称", "不存在的城市名称")).thenReturn(null);

        assertThrows(BizException.class, () -> service.discover(null, "不存在的城市名称"));
    }

    @Test
    void discoversCityFromWgs84Location() {
        when(geocodingProvider.reverseGeocode(108.94, 34.34, true)).thenReturn(
                new AmapGeocodingProvider.Result("陕西省西安市", "陕西省", "西安市", "碑林区", "610100",
                        new AmapPoiProvider.Coordinate(108.945, 34.341)));
        when(cityResolver.resolve("610100", "西安")).thenReturn(Optional.empty());
        CityContextVO context = service.discoverByLocation(108.94, 34.34, true);

        assertEquals("西安", context.getCity().getCityName());
        assertEquals("610100", context.getAdcode());
        assertEquals("discovered", context.getSource());
    }
}
