package com.guido.scenicai.integration.amap;

import com.fasterxml.jackson.databind.JsonNode;
import com.guido.scenicai.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AmapGeocodingProvider {
    private final AmapWebServiceClient client;

    public Result geocode(String address, String city) {
        JsonNode first = client.get("/v3/geocode/geo", Map.of("address", address, "city", city))
                .path("geocodes").path(0);
        AmapPoiProvider.Coordinate coordinate = AmapPoiProvider.Coordinate.parse(first.path("location").asText());
        return coordinate == null ? null : new Result(first.path("formatted_address").asText(),
                text(first.path("province")), text(first.path("city")), text(first.path("district")),
                first.path("adcode").asText(), coordinate);
    }

    public Result reverseGeocode(double longitude, double latitude, boolean wgs84) {
        AmapPoiProvider.Coordinate coordinate = new AmapPoiProvider.Coordinate(longitude, latitude);
        if (wgs84) {
            coordinate = toGcj02(coordinate);
        }
        JsonNode regeo = client.get("/v3/geocode/regeo", Map.of(
                "location", location(coordinate), "extensions", "base", "radius", "1000"))
                .path("regeocode");
        JsonNode component = regeo.path("addressComponent");
        String province = text(component.path("province"));
        String city = text(component.path("city"));
        if (city.isBlank()) city = province;
        String adcode = component.path("adcode").asText();
        return adcode.isBlank() ? null : new Result(regeo.path("formatted_address").asText(),
                province, city, text(component.path("district")), adcode, coordinate);
    }

    private String location(AmapPoiProvider.Coordinate coordinate) {
        return coordinate.longitude() + "," + coordinate.latitude();
    }

    /**
     * WGS84 → GCJ02 只是精度优化：高德坐标转换接口不可用（未开通 / 超限 / 网络异常）时回退为原始坐标，
     * 让逆地理编码仍能完成，避免整条「定位当前位置」链路因为一个可选接口而失败。
     */
    private AmapPoiProvider.Coordinate toGcj02(AmapPoiProvider.Coordinate coordinate) {
        try {
            JsonNode converted = client.get("/v3/assistant/coordinate/convert",
                    Map.of("locations", location(coordinate), "coordsys", "gps"));
            AmapPoiProvider.Coordinate parsed = AmapPoiProvider.Coordinate.parse(converted.path("locations").asText());
            if (parsed == null) {
                log.warn("高德坐标转换返回空结果，改用原始 WGS84 坐标继续逆地理编码");
                return coordinate;
            }
            return parsed;
        } catch (BizException e) {
            log.warn("高德坐标转换失败（{}），改用原始 WGS84 坐标继续逆地理编码", e.getMsg());
            return coordinate;
        }
    }

    private String text(JsonNode value) {
        if (value.isArray()) return value.isEmpty() ? "" : value.path(0).asText();
        return value.asText();
    }

    public record Result(String formattedAddress, String province, String city, String district,
                         String adcode, AmapPoiProvider.Coordinate coordinates) {}
}
