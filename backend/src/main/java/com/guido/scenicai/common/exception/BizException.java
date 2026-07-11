package com.guido.scenicai.common.exception;

/**
 * BusinessException 的兼容别名，保留架构文档中的 BizException 命名。
 */
public class BizException extends BusinessException {

    public BizException(int code, String message) {
        super(code, message);
    }

    public BizException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public BizException(String message) {
        this(400, message);
    }

    public static BizException notFound(String message) {
        return new BizException(404, message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(401, message);
    }

    public static BizException forbidden(String message) {
        return new BizException(403, message);
    }
}
