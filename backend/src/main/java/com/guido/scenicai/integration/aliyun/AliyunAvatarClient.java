package com.guido.scenicai.integration.aliyun;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guido.scenicai.common.constant.LogTypeEnum;
import com.guido.scenicai.common.constant.ServiceType;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.common.result.ResultCode;
import com.guido.scenicai.integration.common.AiCallLogHelper;
import com.guido.scenicai.integration.common.AiConfigLoader;
import com.guido.scenicai.integration.common.AiConfigSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AliyunAvatarClient {

    private static final String VERSION = "2024-03-13";
    private static final DateTimeFormatter POP_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC);

    private final AiConfigLoader aiConfigLoader;
    private final ObjectMapper objectMapper;
    private final AiCallLogHelper aiCallLogHelper;

    public AvatarStreamResult createStream(String text, String audioUrl) {
        return createStream(text, audioUrl, null, null);
    }

    public AvatarStreamResult createStream(String text, String audioUrl, String projectId, String voice) {
        long start = System.nanoTime();
        AiConfigSnapshot config = aiConfigLoader.loadDefault(ServiceType.AVATAR);
        if (!StringUtils.hasText(text)) {
            throw new BizException(400, "数字人播报文本不能为空");
        }
        validateConfig(config);
        try {
            JsonNode extra = extraConfig(config);
            String resolvedProjectId = resolveProjectId(config, extra, projectId);
            String responseBody = startAvatarSession(config, extra, resolvedProjectId);
            JsonNode body = bodyNode(responseBody);
            String streamUrl = firstText(body, "streamUrl", "playUrl", "pullUrl", "rtcPullUrl");
            if (!StringUtils.hasText(streamUrl)) {
                String webSocketUrl = firstText(body, "webSocketUrl");
                String channelToken = firstText(body, "channelToken");
                throw new BizException(ResultCode.AI_SERVICE_ERROR.getCode(),
                        "数字人会话已启动但未返回可播放 streamUrl；webSocketUrl="
                                + hasValue(webSocketUrl) + ",channelToken=" + hasValue(channelToken));
            }
            logSuccess("projectId=" + maskTail(resolvedProjectId) + ",textLength=" + text.length()
                    + ",audioUrl=" + hasValue(audioUrl) + ",voice=" + hasValue(voice), streamUrl, start);
            return new AvatarStreamResult(streamUrl, responseBody);
        } catch (BizException e) {
            logFailure("textLength=" + text.length() + ",projectId=" + hasValue(projectId), start, e);
            throw e;
        } catch (Exception e) {
            BizException wrapped = new BizException(ResultCode.AI_SERVICE_TIMEOUT.getCode(), "阿里云数字人调用异常", e);
            logFailure("textLength=" + text.length() + ",projectId=" + hasValue(projectId), start, wrapped);
            throw wrapped;
        }
    }

    private String startAvatarSession(AiConfigSnapshot config, JsonNode extra, String projectId) throws Exception {
        Map<String, String> params = openApiParams(config, "StartAvatarSession");
        params.put("ProjectId", projectId);
        params.put("RequestId", UUID.randomUUID().toString());
        addOptional(params, "CustomPushUrl", text(extra, "customPushUrl", null));
        addOptional(params, "CustomUserId", text(extra, "customUserId", null));
        addOptional(params, "ChannelToken", text(extra, "channelToken", null));
        params.put("Signature", signature(params, config.getAccessKeySecret()));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint(config) + "/?" + query(params)))
                .timeout(java.time.Duration.ofMillis(timeoutMs(config)))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BizException(ResultCode.AI_SERVICE_ERROR.getCode(),
                    "阿里云数字人启动会话失败：" + response.statusCode() + " " + limit(response.body(), 180));
        }
        return response.body();
    }

    private Map<String, String> openApiParams(AiConfigSnapshot config, String action) {
        Map<String, String> params = new TreeMap<>();
        params.put("AccessKeyId", config.getAccessKeyId());
        params.put("Action", action);
        params.put("Format", "JSON");
        params.put("RegionId", config.getRegion());
        params.put("SignatureMethod", "HMAC-SHA1");
        params.put("SignatureNonce", UUID.randomUUID().toString());
        params.put("SignatureVersion", "1.0");
        params.put("Timestamp", POP_TIME_FORMATTER.format(Instant.now()));
        params.put("Version", VERSION);
        return params;
    }

    private String signature(Map<String, String> params, String accessKeySecret) throws Exception {
        String stringToSign = "GET&%2F&" + percentEncode(query(params));
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec((accessKeySecret + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        return Base64.getEncoder().encodeToString(mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));
    }

    private JsonNode bodyNode(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode body = root.path("Body");
        return body.isMissingNode() ? root : body;
    }

    private JsonNode extraConfig(AiConfigSnapshot config) throws Exception {
        if (!StringUtils.hasText(config.getExtraConfig())) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(config.getExtraConfig());
    }

    private String resolveProjectId(AiConfigSnapshot config, JsonNode extra, String projectId) {
        String resolved = firstNonBlank(projectId, text(extra, "projectId", null), config.getAppKey());
        if (!StringUtils.hasText(resolved)) {
            throw new BizException(ResultCode.AI_CONFIG_MISSING.getCode(), "数字人 projectId 缺失");
        }
        return resolved;
    }

    private void validateConfig(AiConfigSnapshot config) {
        if (config == null || !StringUtils.hasText(config.getAccessKeyId())
                || !StringUtils.hasText(config.getAccessKeySecret())
                || !StringUtils.hasText(config.getRegion())) {
            throw new BizException(ResultCode.AI_CONFIG_MISSING.getCode(), "数字人配置缺失或未启用");
        }
    }

    private String endpoint(AiConfigSnapshot config) {
        if (StringUtils.hasText(config.getBaseUrl())) {
            return config.getBaseUrl().endsWith("/")
                    ? config.getBaseUrl().substring(0, config.getBaseUrl().length() - 1)
                    : config.getBaseUrl();
        }
        return "https://intelligentcreation." + config.getRegion() + ".aliyuncs.com";
    }

    private void addOptional(Map<String, String> params, String key, String value) {
        if (StringUtils.hasText(value)) {
            params.put(key, value);
        }
    }

    private String query(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> percentEncode(entry.getKey()) + "=" + percentEncode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String percentEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }

    private void logSuccess(String requestSummary, String streamUrl, long start) {
        try {
            aiCallLogHelper.logSuccess(LogTypeEnum.AVATAR, "阿里云",
                    requestSummary, "streamUrl=" + hasValue(streamUrl), elapsedMs(start), "system");
        } catch (Exception e) {
            log.warn("数字人调用日志写入失败", e);
        }
    }

    private void logFailure(String requestSummary, long start, Exception exception) {
        try {
            aiCallLogHelper.logFailure(LogTypeEnum.AVATAR, "阿里云",
                    requestSummary, elapsedMs(start), exception.getMessage(), "system");
        } catch (Exception e) {
            log.warn("数字人失败日志写入失败", e);
        }
    }

    private String firstText(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node == null ? null : node.get(key);
            if (value != null && StringUtils.hasText(value.asText())) {
                return value.asText();
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode node, String key, String defaultValue) {
        JsonNode value = node == null ? null : node.get(key);
        return value == null || value.isNull() || !StringUtils.hasText(value.asText()) ? defaultValue : value.asText();
    }

    private int timeoutMs(AiConfigSnapshot config) {
        return config.getTimeoutMs() == null || config.getTimeoutMs() <= 0 ? 60000 : config.getTimeoutMs();
    }

    private int elapsedMs(long start) {
        return Math.toIntExact((System.nanoTime() - start) / 1_000_000L);
    }

    private String hasValue(String value) {
        return String.valueOf(StringUtils.hasText(value));
    }

    private String maskTail(String value) {
        if (!StringUtils.hasText(value)) {
            return "false";
        }
        return value.length() <= 4 ? "****" : "****" + value.substring(value.length() - 4);
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
