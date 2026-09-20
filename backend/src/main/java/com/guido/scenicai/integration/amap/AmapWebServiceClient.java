package com.guido.scenicai.integration.amap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guido.scenicai.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.MDC;

@Component
@RequiredArgsConstructor
@Slf4j
public class AmapWebServiceClient {
    private static final String BASE_URL = "https://restapi.amap.com";
    private final ObjectMapper objectMapper;
    private final Map<String, CacheEntry> responseCache = new ConcurrentHashMap<>();

    @Value("${amap.web-service-key:}")
    private String key;
    @Value("${amap.cache.geocode-ttl:24h}") private Duration geocodeTtl;
    @Value("${amap.cache.poi-ttl:30m}") private Duration poiTtl;
    @Value("${amap.cache.weather-ttl:10m}") private Duration weatherTtl;
    @Value("${amap.cache.route-ttl:5m}") private Duration routeTtl;

    public JsonNode get(String api, Map<String, String> parameters) {
        if (!StringUtils.hasText(key)) {
            throw new BizException(1005, "AMAP_WEB_SERVICE_KEY_NOT_CONFIGURED");
        }
        try {
            String cacheKey = api + "?" + parameters.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .reduce((a, b) -> a + "&" + b).orElse("");
            CacheEntry cached = responseCache.get(cacheKey);
            if (cached != null && cached.expiresAt() > System.currentTimeMillis()) {
                increment("amapCacheHitCount");
                log.info("[AMAP_CACHE_HIT] executionId={} tool={} endpoint={} city={} cacheHit=true",
                        executionId(), toolFor(api), api, parameters.getOrDefault("city", parameters.getOrDefault("region", "")));
                return cached.body();
            }
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + api).queryParam("key", key);
            parameters.forEach(builder::queryParam);
            URI uri = builder.build().encode().toUri();
            log.info("[AMAP_CALL] executionId={} tool={} endpoint={} city={} cacheHit=false",
                    executionId(),
                    toolFor(api), api, parameters.getOrDefault("city", parameters.getOrDefault("region", "")));
            increment("amapCallCount");
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(12)).GET().build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode body = objectMapper.readTree(response.body());
            if (response.statusCode() != 200 || !"1".equals(body.path("status").asText())) {
                String info = body.path("info").asText("高德服务调用失败");
                String infocode = body.path("infocode").asText("");
                log.warn("高德 API 失败: api={}, httpStatus={}, info={}, infocode={}", api, response.statusCode(), info, infocode);
                if (info.contains("DAILY_QUERY_OVER_LIMIT")) {
                    throw new BizException(1002, "AMAP_DAILY_QUERY_OVER_LIMIT");
                }
                throw new BizException(1002, "AMAP_API_ERROR: " + info + " (" + infocode + ")");
            }
            responseCache.put(cacheKey, new CacheEntry(body, System.currentTimeMillis() + ttlFor(api).toMillis()));
            return body;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("高德 API 异常: api={}, type={}", api, e.getClass().getSimpleName());
            throw new BizException(1001, "AMAP_API_REQUEST_FAILED", e);
        }
    }

    private String executionId() { return MDC.get("amapExecutionId") == null ? "standalone" : MDC.get("amapExecutionId"); }
    private void increment(String key) {
        String value = MDC.get(key);
        MDC.put(key, String.valueOf(value == null ? 1 : Integer.parseInt(value) + 1));
    }
    private record CacheEntry(JsonNode body, long expiresAt) {}

    private Duration ttlFor(String api) {
        if (api.startsWith("/v5/place")) return poiTtl;
        if (api.startsWith("/v5/direction")) return routeTtl;
        if (api.startsWith("/v3/weather")) return weatherTtl;
        return geocodeTtl;
    }

    private String toolFor(String api) {
        if (api.startsWith("/v5/place")) return "amap.poi";
        if (api.startsWith("/v5/direction")) return "amap.route";
        if (api.startsWith("/v3/weather")) return "amap.weather";
        if (api.startsWith("/v3/geocode") || api.startsWith("/v3/assistant/coordinate")) return "amap.geocode";
        return "amap.web-service";
    }
}
