package com.guido.scenicai.common.exception;

import com.guido.scenicai.common.result.ResultCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final int code;
    private final String msg;

    public BusinessException(ResultCode resultCode) {
        this(resultCode.getCode(), resultCode.getMsg());
    }

    public BusinessException(ResultCode resultCode, String msg) {
        this(resultCode.getCode(), msg);
    }

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public BusinessException(int code, String msg, Throwable cause) {
        super(msg, cause);
        this.code = code;
        this.msg = msg;
    }
}
