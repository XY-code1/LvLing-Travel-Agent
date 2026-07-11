package com.guido.scenicai.common.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 多账号体系配置 + 路由拦截。
 * admin 和 tourist 两套独立账号线，互不越权。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @PostConstruct
    public void initStpLogic() {
        // 注册两套 StpLogic，Sa-Token 内部据此区分账号类型（token 存储命名空间隔离）
        cn.dev33.satoken.SaManager.putStpLogic(StpAdminUtil.stpLogic);
        cn.dev33.satoken.SaManager.putStpLogic(StpTouristUtil.stpLogic);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        /*
         * 管理端路由：/api/admin/** 需要 admin 登录态。
         * /api/admin/login 放行（免登录）。
         */
        registry.addInterceptor(new SaInterceptor(handle -> {
                    SaRouter.match("/api/admin/**")
                            .notMatch("/api/admin/login")
                            .check(r -> StpAdminUtil.stpLogic.checkLogin());
                }))
                .addPathPatterns("/api/admin/**");

        /*
         * 游客端路由：/api/tourist/** 需要 tourist 登录态。
         * /api/tourist/auth/register、/api/tourist/auth/login 放行（免登录）。
         */
        registry.addInterceptor(new SaInterceptor(handle -> {
                    SaRouter.match("/api/tourist/**")
                            .notMatch("/api/tourist/auth/register",
                                      "/api/tourist/auth/login")
                            .check(r -> StpTouristUtil.stpLogic.checkLogin());
                }))
                .addPathPatterns("/api/tourist/**");
    }
}
