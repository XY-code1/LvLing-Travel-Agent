package com.guido.scenicai.common.config;

import cn.dev33.satoken.stp.StpLogic;

/**
 * Sa-Token 管理员账号体系（账号类型 admin）。
 * 与游客端完全隔离，token 互不通用。
 */
public final class StpAdminUtil {

    private StpAdminUtil() {
    }

    public static final String TYPE = "admin";

    public static final StpLogic stpLogic = new StpLogic(TYPE);
}
