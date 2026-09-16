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

@Component
@RequiredArgsConstructor
@Slf4j
public class AmapWebServiceClient {
    private static final String BASE_URL = "https://restapi.amap.com";
    private final ObjectMapper objectMapper;

    @Value("${amap.web-service-key:}")
    private String key;

    public JsonNode get(String api, Map<String, String> parameters) {
        if (!StringUtils.hasText(key)) {
            throw new BizException(1005, "AMAP_WEB_SERVICE_KEY_NOT_CONFIGURED");
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + api).queryParam("key", key);
            parameters.forEach(builder::queryParam);
            URI uri = builder.build().encode().toUri();
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(12)).GET().build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode body = objectMapper.readTree(response.body());
            if (response.statusCode() != 200 || !"1".equals(body.path("status").asText())) {
                String info = body.path("info").asText("高德服务调用失败");
                String infocode = body.path("infocode").asText("");
                log.warn("高德 API 失败: api={}, httpStatus={}, info={}, infocode={}", api, response.statusCode(), info, infocode);
                throw new BizException(1002, "AMAP_API_ERROR: " + info + " (" + infocode + ")");
            }
            return body;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("高德 API 异常: api={}, type={}", api, e.getClass().getSimpleName());
            throw new BizException(1001, "AMAP_API_REQUEST_FAILED", e);
        }
    }
}
