package com.guido.scenicai.integration.common;

import com.guido.scenicai.common.util.MaskUtil;
import com.guido.scenicai.module.log.entity.SysLogEntity;
import com.guido.scenicai.module.log.service.SysLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Unified AI call logger for the integration layer.
 *
 * <p>All AI service calls (LLM, ASR, TTS, AVATAR, VISION, KB)
 * should use this helper to record call metadata into sys_log.
 *
 * <p>Desensitization: requestSummary and responseSummary are automatically
 * sanitized via {@link MaskUtil#maskSensitive(String)} — never log raw API keys,
 * AccessKeySecret, or passwords.
 *
 * <h3>Usage (from any integration client):</h3>
 * <pre>{@code
 *   aiCallLogHelper.logSuccess(
 *       LogType.ASR,
 *       "阿里云",
 *       "audioDuration=3200ms, format=pcm",
 *       "{\"result\":\"你好，欢迎来到景区\"}",
 *       450,
 *       "admin"
 *   );
 * }</pre>
 */
@Component
@RequiredArgsConstructor
public class AiCallLogHelper {

    private final SysLogService sysLogService;

    /**
     * Record an AI service call to sys_log.
     *
     * @param logType         LOGIN/OPERATION/CHAT/ASR/TTS/AVATAR/VISION/KB/EXCEPTION
     * @param serviceProvider e.g. "阿里云", "DeepSeek", "本地 RAG"
     * @param requestSummary  desensitized request info — no raw keys/secrets
     * @param responseSummary desensitized response info — no raw keys/secrets
     * @param costMs          call duration in milliseconds
     * @param success         whether the call succeeded
     * @param errorMsg        error message if failed, null if success
     * @param operator        operator identifier (admin username or tourist id)
     */
    public void log(String logType, String serviceProvider,
                    String requestSummary, String responseSummary,
                    int costMs, boolean success, String errorMsg,
                    String operator) {
        SysLogEntity entity = new SysLogEntity();
        entity.setLogType(logType);
        entity.setServiceProvider(serviceProvider);
        entity.setRequestSummary(MaskUtil.maskSensitive(requestSummary));
        entity.setResponseSummary(MaskUtil.maskSensitive(responseSummary));
        entity.setCostMs(costMs);
        entity.setSuccess(success ? 1 : 0);
        entity.setErrorMsg(truncate(errorMsg, 1000));
        entity.setOperator(operator);
        entity.setCreateTime(LocalDateTime.now());
        sysLogService.save(entity);
    }

    /**
     * Convenience for successful AI calls.
     */
    public void logSuccess(String logType, String serviceProvider,
                           String requestSummary, String responseSummary,
                           int costMs, String operator) {
        log(logType, serviceProvider, requestSummary, responseSummary,
                costMs, true, null, operator);
    }

    /**
     * Convenience for failed AI calls.
     */
    public void logFailure(String logType, String serviceProvider,
                           String requestSummary, int costMs,
                           String errorMsg, String operator) {
        log(logType, serviceProvider, requestSummary, null,
                costMs, false, errorMsg, operator);
    }

    private String truncate(String msg, int maxLen) {
        if (msg == null) {
            return null;
        }
        return msg.length() > maxLen ? msg.substring(0, maxLen) : msg;
    }
}
