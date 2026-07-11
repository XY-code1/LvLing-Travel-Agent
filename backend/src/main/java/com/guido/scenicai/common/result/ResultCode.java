package com.guido.scenicai.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "success"),
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "未登录/令牌失效"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    AI_SERVICE_TIMEOUT(1001, "外部 AI 服务超时"),
    AI_SERVICE_ERROR(1002, "外部 AI 服务返回错误"),
    KB_NOT_HIT(1003, "知识库未命中"),
    FILE_INVALID(1004, "文件类型/大小不合法"),
    AI_CONFIG_MISSING(1005, "AI 配置缺失或未启用");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
