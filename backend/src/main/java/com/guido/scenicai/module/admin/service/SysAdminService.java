package com.guido.scenicai.module.admin.service;

import com.guido.scenicai.module.admin.dto.AdminLoginDTO;
import com.guido.scenicai.module.admin.vo.AdminInfoVO;
import com.guido.scenicai.module.admin.vo.AdminLoginVO;

public interface SysAdminService {

    /**
     * 管理员登录。
     *
     * @param dto       用户名 + 密码
     * @param ip        客户端 IP（用于日志）
     * @param userAgent 浏览器 User-Agent（用于日志）
     */
    AdminLoginVO login(AdminLoginDTO dto, String ip, String userAgent);

    void logout();

    AdminInfoVO getCurrentAdminInfo();
}
