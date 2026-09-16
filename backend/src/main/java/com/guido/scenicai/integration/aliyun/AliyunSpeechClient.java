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
import org.springframework.beans.factory.annotation.Value;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AliyunSpeechClient {

    private static final String TOKEN_ACTION = "CreateToken";
    private static final String TOKEN_VERSION = "2019-02-28";
    private static final DateTimeFormatter POP_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC);

    private final AiConfigLoader aiConfigLoader;
    private final ObjectMapper objectMapper;
    private final AiCallLogHelper aiCallLogHelper;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    public AsrResult recognize(byte[] audioBytes, String fileName) {
        long start = System.nanoTime();
        AiConfigSnapshot config = aiConfigLoader.loadDefault(ServiceType.ASR);
        if (audioBytes == null || audioBytes.length == 0) {
            throw new BizException(400, "ASR 音频不能为空");
        }
        validateAliConfig(config, "ASR");
        try {
            JsonNode extra = extraConfig(config);
            String format = text(extra, "asrFormat", detectAudioFormat(fileName));
            int sampleRate = number(extra, "asrSampleRate", 16000);
            URI uri = asrUri(config, extra, format, sampleRate);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofMillis(timeoutMs(config)))
                    .header("X-NLS-Token", speechToken(config, extra))
                    .header("Content-Type", "application/octet-stream")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(audioBytes))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            AsrResult result = parseAsrResponse(response);
            logSuccess(LogTypeEnum.ASR, asrSummary(audioBytes, format, sampleRate),
                    "textLength=" + result.getText().length(), start);
            return result;
        } catch (BizException e) {
            logFailure(LogTypeEnum.ASR, audioSummary(audioBytes), start, e);
            throw e;
        } catch (Exception e) {
            BizException wrapped = new BizException(ResultCode.AI_SERVICE_TIMEOUT.getCode(), "阿里云 ASR 调用异常", e);
            logFailure(LogTypeEnum.ASR, audioSummary(audioBytes), start, wrapped);
            throw wrapped;
        }
    }

    public TtsResult synthesize(String text) {
        return synthesize(text, null, null);
    }

    public TtsResult synthesize(String text, String voiceOverride, Integer speechRateOverride) {
        return synthesize(text, voiceOverride, speechRateOverride, null);
    }

    public TtsResult synthesize(String text, String voiceOverride, Integer speechRateOverride,
                                Integer pitchRateOverride) {
        long start = System.nanoTime();
        AiConfigSnapshot config = aiConfigLoader.loadDefault(ServiceType.TTS);
        if (!StringUtils.hasText(text)) {
            throw new BizException(400, "TTS 文本不能为空");
        }
        validateAliConfig(config, "TTS");
        try {
            JsonNode extra = extraConfig(config);
            String format = text(extra, "ttsFormat", "mp3");
            String databaseVoice = text(extra, "voice", voice(config));
            String finalVoice = StringUtils.hasText(voiceOverride)
                    ? normalizeVoiceCode(voiceOverride)
                    : databaseVoice;
            if (!StringUtils.hasText(finalVoice)) {
                finalVoice = "xiaoyun";
            }
            log.info("[TTS Provider] db default voice={}, request voiceId={}, final voice used={}",
                    databaseVoice, voiceOverride, finalVoice);
            log.info("[Aliyun TTS] final voice={}", finalVoice);
            Map<String, Object> body = ttsBody(config, extra, text, format, finalVoice,
                    speechRateOverride, pitchRateOverride);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(resolveEndpoint(config, extra, "ttsUrl", "/stream/v1/tts")))
                    .timeout(Duration.ofMillis(timeoutMs(config)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<byte[]> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofByteArray());
            String audioUrl = storeAudio(response, format, finalVoice);
            logSuccess(LogTypeEnum.TTS, "textLength=" + text.length() + ",format=" + format,
                    "audioUrl=" + audioUrl, start);
            return new TtsResult(audioUrl, "audioBytes=" + response.body().length);
        } catch (BizException e) {
            logFailure(LogTypeEnum.TTS, "textLength=" + text.length(), start, e);
            throw e;
        } catch (Exception e) {
            BizException wrapped = new BizException(ResultCode.AI_SERVICE_TIMEOUT.getCode(), "阿里云 TTS 调用异常", e);
            logFailure(LogTypeEnum.TTS, "textLength=" + text.length(), start, wrapped);
            throw wrapped;
        }
    }

    private AsrResult parseAsrResponse(HttpResponse<String> response) throws Exception {
        JsonNode root = objectMapper.readTree(response.body());
        int status = root.path("status").asInt(response.statusCode());
        if (response.statusCode() != 200 || status != 20000000) {
            String message = root.path("message").asText(response.body());
            throw new BizException(ResultCode.AI_SERVICE_ERROR.getCode(), "阿里云 ASR 失败：" + limit(message, 180));
        }
        String result = root.path("result").asText();
        if (!StringUtils.hasText(result)) {
            throw new BizException(ResultCode.AI_SERVICE_ERROR.getCode(), "阿里云 ASR 响应缺少识别文本");
        }
        return new AsrResult(result, response.body());
    }

    private String storeAudio(HttpResponse<byte[]> response, String format, String requestedVoice) throws Exception {
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        if (response.statusCode() != 200 || contentType.toLowerCase().contains("json")) {
            String errorResponse = limit(new String(response.body(), StandardCharsets.UTF_8), 180);
            log.error("[TTS Voice Error] requested voice={}, reason=Aliyun rejected TTS request, response={}",
                    requestedVoice, errorResponse);
            throw new BizException(ResultCode.AI_SERVICE_ERROR.getCode(),
                    "阿里云 TTS 失败：" + errorResponse);
        }
        Path dir = Path.of(uploadPath, "audio");
        Files.createDirectories(dir);
        String safeFormat = safeAudioFormat(format);
        String fileName = UUID.randomUUID() + "." + safeFormat;
        Files.write(dir.resolve(fileName), response.body());
        return "/files/audio/" + fileName;
    }

    private URI asrUri(AiConfigSnapshot config, JsonNode extra, String format, int sampleRate) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("appkey", config.getAppKey());
        params.put("format", format);
        params.put("sample_rate", String.valueOf(sampleRate));
        params.put("enable_punctuation_prediction", String.valueOf(bool(extra, "enablePunctuationPrediction", true)));
        params.put("enable_inverse_text_normalization", String.valueOf(bool(extra, "enableInverseTextNormalization", true)));
        params.put("enable_voice_detection", String.valueOf(bool(extra, "enableVoiceDetection", true)));
        String endpoint = resolveEndpoint(config, extra, "asrUrl", "/stream/v1/asr");
        return URI.create(endpoint + "?" + query(params));
    }

    private Map<String, Object> ttsBody(AiConfigSnapshot config, JsonNode extra, String text, String format,
                                        String finalVoice, Integer speechRateOverride,
                                        Integer pitchRateOverride) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("appkey", config.getAppKey());
        body.put("token", speechToken(config, extra));
        body.put("text", text);
        body.put("format", format);
        body.put("sample_rate", number(extra, "ttsSampleRate", 16000));
        body.put("voice", finalVoice);
        body.put("volume", number(extra, "volume", 50));
        body.put("speech_rate", speechRateOverride == null ? number(extra, "speechRate", 0) : speechRateOverride);
        body.put("pitch_rate", pitchRateOverride == null ? number(extra, "pitchRate", 0) : pitchRateOverride);
        return body;
    }

    private String normalizeVoiceCode(String voice) {
        String raw = voice == null ? "" : voice.trim();
        if (raw.contains("男")) {
            return "xiaogang";
        }
        if (raw.contains("女")) {
            return "xiaoyun";
        }
        return raw;
    }

    private String speechToken(AiConfigSnapshot config, JsonNode extra) {
        String configuredToken = text(extra, "token", null);
        if (StringUtils.hasText(configuredToken)) {
            return configuredToken;
        }
        return createToken(config);
    }

    private String createToken(AiConfigSnapshot config) {
        try {
            URI uri = tokenUri(config);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofMillis(timeoutMs(config)))
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            String token = root.path("Token").path("Id").asText();
            if (response.statusCode() != 200 || !StringUtils.hasText(token)) {
                throw new BizException(ResultCode.AI_SERVICE_ERROR.getCode(),
                        "阿里云 NLS Token 获取失败：" + limit(response.body(), 180));
            }
            return token;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ResultCode.AI_SERVICE_TIMEOUT.getCode(), "阿里云 NLS Token 获取异常", e);
        }
    }

    private URI tokenUri(AiConfigSnapshot config) throws Exception {
        Map<String, String> params = new TreeMap<>();
        params.put("AccessKeyId", config.getAccessKeyId());
        params.put("Action", TOKEN_ACTION);
        params.put("Format", "JSON");
        params.put("RegionId", config.getRegion());
        params.put("SignatureMethod", "HMAC-SHA1");
        params.put("SignatureNonce", UUID.randomUUID().toString());
        params.put("SignatureVersion", "1.0");
        params.put("Timestamp", POP_TIME_FORMATTER.format(Instant.now()));
        params.put("Version", TOKEN_VERSION);
        params.put("Signature", signature(params, config.getAccessKeySecret()));
        String host = "https://nls-meta." + config.getRegion() + ".aliyuncs.com/";
        return URI.create(host + "?" + query(params));
    }

    private String signature(Map<String, String> params, String accessKeySecret) throws Exception {
        String stringToSign = "GET&%2F&" + percentEncode(query(params));
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec((accessKeySecret + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        return Base64.getEncoder().encodeToString(mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));
    }

    private String resolveEndpoint(AiConfigSnapshot config, JsonNode extra, String key, String path) {
        String explicit = text(extra, key, null);
        if (StringUtils.hasText(explicit)) {
            return explicit;
        }
        String base = StringUtils.hasText(config.getBaseUrl())
                ? config.getBaseUrl()
                : "https://nls-gateway-" + config.getRegion() + ".aliyuncs.com";
        String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return normalized.endsWith(path) ? normalized : normalized + path;
    }

    private JsonNode extraConfig(AiConfigSnapshot config) throws Exception {
        if (!StringUtils.hasText(config.getExtraConfig())) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(config.getExtraConfig());
    }

    private void validateAliConfig(AiConfigSnapshot config, String scene) {
        if (config == null || !StringUtils.hasText(config.getAccessKeyId())
                || !StringUtils.hasText(config.getAccessKeySecret())
                || !StringUtils.hasText(config.getAppKey())
                || !StringUtils.hasText(config.getRegion())) {
            throw new BizException(ResultCode.AI_CONFIG_MISSING.getCode(), scene + " 配置缺失或未启用");
        }
    }

    private void logSuccess(String logType, String requestSummary, String responseSummary, long start) {
        try {
            aiCallLogHelper.logSuccess(logType, "阿里云", requestSummary, responseSummary, elapsedMs(start), "system");
        } catch (Exception e) {
            log.warn("阿里云语音调用日志写入失败: logType={}", logType, e);
        }
    }

    private void logFailure(String logType, String requestSummary, long start, Exception exception) {
        try {
            aiCallLogHelper.logFailure(logType, "阿里云", requestSummary,
                    elapsedMs(start), exception.getMessage(), "system");
        } catch (Exception e) {
            log.warn("阿里云语音失败日志写入失败: logType={}", logType, e);
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

    private String detectAudioFormat(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "mp3";
        }
        return safeAudioFormat(fileName.substring(fileName.lastIndexOf('.') + 1));
    }

    private String safeAudioFormat(String format) {
        String value = StringUtils.hasText(format) ? format.toLowerCase() : "mp3";
        return switch (value) {
            case "pcm", "wav", "mp3", "opus", "speex", "amr", "aac" -> value;
            default -> "mp3";
        };
    }

    private String voice(AiConfigSnapshot config) {
        return StringUtils.hasText(config.getProtocol()) ? config.getProtocol() : "xiaoyun";
    }

    private String text(JsonNode node, String key, String defaultValue) {
        JsonNode value = node == null ? null : node.get(key);
        return value == null || value.isNull() || !StringUtils.hasText(value.asText()) ? defaultValue : value.asText();
    }

    private int number(JsonNode node, String key, int defaultValue) {
        JsonNode value = node == null ? null : node.get(key);
        return value == null || !value.canConvertToInt() ? defaultValue : value.asInt();
    }

    private boolean bool(JsonNode node, String key, boolean defaultValue) {
        JsonNode value = node == null ? null : node.get(key);
        return value == null || !value.isBoolean() ? defaultValue : value.asBoolean();
    }

    private int timeoutMs(AiConfigSnapshot config) {
        return config.getTimeoutMs() == null || config.getTimeoutMs() <= 0 ? 60000 : config.getTimeoutMs();
    }

    private int elapsedMs(long start) {
        return Math.toIntExact((System.nanoTime() - start) / 1_000_000L);
    }

    private String asrSummary(byte[] audioBytes, String format, int sampleRate) {
        return audioSummary(audioBytes) + ",format=" + format + ",sampleRate=" + sampleRate;
    }

    private String audioSummary(byte[] audioBytes) {
        return "audioBytes=" + (audioBytes == null ? 0 : audioBytes.length);
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
