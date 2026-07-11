package com.guido.scenicai.common.interceptor;

import cn.hutool.json.JSONUtil;
import com.guido.scenicai.common.config.StpAdminUtil;
import com.guido.scenicai.common.constant.LogType;
import com.guido.scenicai.common.util.MaskUtil;
import com.guido.scenicai.module.log.entity.SysLogEntity;
import com.guido.scenicai.module.log.service.SysLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * AOP aspect that intercepts methods annotated with {@link OperationLog}
 * and records execution info to sys_log (log_type=OPERATION).
 *
 * <p>Desensitization: request_summary and response_summary are sanitized via
 * {@link MaskUtil#maskSensitive(String)} before storage.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private static final int SUMMARY_MAX_LEN = 4000;

    private final SysLogService sysLogService;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();

        SysLogEntity sysLog = new SysLogEntity();
        sysLog.setLogType(LogType.OPERATION);
        sysLog.setBizDesc(operationLog.value());

        // Extract operator and IP from request context
        resolveOperator(sysLog);

        // Build request summary from method signature + args (desensitized)
        buildRequestSummary(joinPoint, sysLog);

        Object result;
        try {
            result = joinPoint.proceed();
            sysLog.setSuccess(1);
            buildResponseSummary(result, sysLog);
        } catch (Throwable e) {
            sysLog.setSuccess(0);
            sysLog.setErrorMsg(truncate(e.getMessage(), 1000));
            throw e;
        } finally {
            sysLog.setCostMs((int) (System.currentTimeMillis() - start));
            sysLog.setCreateTime(LocalDateTime.now());
            try {
                sysLogService.save(sysLog);
            } catch (Exception e) {
                log.warn("操作日志写入失败，不影响业务结果：{}", e.getMessage());
            }
        }

        return result;
    }

    private void resolveOperator(SysLogEntity sysLog) {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                sysLog.setIp(request.getRemoteAddr());
                String operator = request.getHeader("X-Operator");
                if (operator == null || operator.isEmpty()) {
                    try {
                        Object loginId = StpAdminUtil.stpLogic.getLoginIdDefaultNull();
                        if (loginId != null) {
                            operator = loginId.toString();
                        }
                    } catch (Exception e) {
                        log.debug("未获取到管理员登录上下文", e);
                    }
                }
                sysLog.setOperator(operator);
            }
        } catch (Exception e) {
            log.debug("无法提取操作人信息", e);
        }
    }

    private void buildRequestSummary(ProceedingJoinPoint joinPoint, SysLogEntity sysLog) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            if (paramNames != null && args != null && args.length > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append(signature.getMethod().getName()).append("(");
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(paramNames[i]).append("=");
                    try {
                        sb.append(toLogText(args[i]));
                    } catch (Exception e) {
                        sb.append(String.valueOf(args[i]));
                    }
                }
                sb.append(")");
                sysLog.setRequestSummary(sanitizeSummary(sb.toString()));
            }
        } catch (Exception e) {
            log.debug("构建操作日志请求摘要失败", e);
        }
    }

    private void buildResponseSummary(Object result, SysLogEntity sysLog) {
        try {
            if (result != null) {
                String json = JSONUtil.toJsonStr(result);
                sysLog.setResponseSummary(sanitizeSummary(json));
            }
        } catch (Exception e) {
            log.debug("构建操作日志响应摘要失败", e);
        }
    }

    private String toLogText(Object arg) {
        if (arg instanceof MultipartFile file) {
            return "MultipartFile{fileName=" + file.getOriginalFilename()
                    + ", size=" + file.getSize()
                    + ", contentType=" + file.getContentType()
                    + "}";
        }
        return JSONUtil.toJsonStr(arg);
    }

    private String sanitizeSummary(String value) {
        return truncate(MaskUtil.maskSensitive(value), SUMMARY_MAX_LEN);
    }

    private String truncate(String msg, int maxLen) {
        if (msg == null) {
            return null;
        }
        return msg.length() > maxLen ? msg.substring(0, maxLen) : msg;
    }
}
