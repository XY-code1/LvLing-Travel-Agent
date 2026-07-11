package com.guido.scenicai.common.config;

import cn.dev33.satoken.stp.StpLogic;

/**
 * Sa-Token 游客账号体系（账号类型 tourist）。
 * 与管理员端完全隔离，token 互不通用。
 */
public final class StpTouristUtil {

    private StpTouristUtil() {
    }

    public static final String TYPE = "tourist";

    public static final StpLogic stpLogic = new StpLogic(TYPE);
}
