package com.guido.scenicai.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guido.scenicai.common.config.StpAdminUtil;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.module.admin.dto.AdminLoginDTO;
import com.guido.scenicai.module.admin.dto.AdminRegisterDTO;
import com.guido.scenicai.module.admin.dto.AdminResetPasswordDTO;
import com.guido.scenicai.module.admin.entity.SysAdmin;
import com.guido.scenicai.module.admin.entity.SysLoginLogEntity;
import com.guido.scenicai.module.admin.mapper.SysAdminMapper;
import com.guido.scenicai.module.admin.mapper.SysLoginLogMapper;
import com.guido.scenicai.module.admin.service.SysAdminService;
import com.guido.scenicai.module.admin.service.CaptchaService;
import com.guido.scenicai.module.admin.vo.AdminInfoVO;
import com.guido.scenicai.module.admin.vo.AdminLoginVO;
import com.guido.scenicai.module.log.entity.SysLogEntity;
import com.guido.scenicai.module.log.mapper.SysLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysAdminServiceImpl implements SysAdminService {

    private final SysAdminMapper sysAdminMapper;
    private final SysLogMapper sysLogMapper;
    private final SysLoginLogMapper sysLoginLogMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;

    /** 找回密码恢复令牌（环境变量 ADMIN_RESET_TOKEN）；为空表示关闭找回密码接口。 */
    @Value("${app.admin.reset-token:}")
    private String resetToken;

    @Override
    public AdminLoginVO login(AdminLoginDTO dto, String ip, String userAgent) {
        captchaService.verify(dto.getCaptchaId(), dto.getCaptchaCode());
        SysAdmin admin = sysAdminMapper.selectOne(
                new LambdaQueryWrapper<SysAdmin>()
                        .eq(SysAdmin::getUsername, dto.getUsername())
        );
        if (admin == null || !passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
            writeLoginFailureLog(dto.getUsername(), ip, userAgent, "账号或密码错误");
            throw new BizException(400, "账号或密码错误");
        }
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            writeLoginFailureLog(dto.getUsername(), ip, userAgent, "账号已被停用");
            throw new BizException(403, "账号已被停用");
        }

        StpAdminUtil.stpLogic.login(admin.getId());

        admin.setLastLoginTime(LocalDateTime.now());
        sysAdminMapper.updateById(admin);

        writeLoginSuccessLog(dto.getUsername(), ip, userAgent);

        String token = StpAdminUtil.stpLogic.getTokenValue();
        AdminInfoVO infoVO = new AdminInfoVO();
        infoVO.setId(admin.getId());
        infoVO.setUsername(admin.getUsername());
        infoVO.setRealName(admin.getRealName());
        infoVO.setRole(admin.getRole());

        log.info("管理员登录成功: username={}", admin.getUsername());
        return new AdminLoginVO(token, infoVO);
    }

    /**
     * 注册管理员：库里还没有任何管理员时允许首次引导注册，
     * 之后必须由已登录的管理员创建账号，避免接口完全开放导致任何人自助拿到后台权限。
     */
    @Override
    public void register(AdminRegisterDTO dto) {
        captchaService.verify(dto.getCaptchaId(), dto.getCaptchaCode());
        Long adminCount = sysAdminMapper.selectCount(null);
        boolean bootstrap = adminCount == null || adminCount == 0;
        if (!bootstrap && !isAdminLoggedIn()) {
            throw new BizException(403, "注册通道已关闭：请由已登录的管理员创建账号");
        }
        if (sysAdminMapper.selectOne(new LambdaQueryWrapper<SysAdmin>().eq(SysAdmin::getUsername, dto.getUsername())) != null) {
            throw new BizException(400, "管理员账号已存在");
        }
        SysAdmin admin = new SysAdmin();
        admin.setUsername(dto.getUsername());
        admin.setPassword(passwordEncoder.encode(dto.getPassword()));
        admin.setRealName(dto.getRealName());
        admin.setRole("admin");
        admin.setStatus(1);
        sysAdminMapper.insert(admin);
    }

    /**
     * 找回密码：除验证码外必须持有服务端配置的恢复令牌（ADMIN_RESET_TOKEN）。
     * 令牌未配置时直接关闭该接口——否则只凭公开的用户名就能重置任意管理员密码。
     */
    @Override
    public void resetPassword(AdminResetPasswordDTO dto) {
        captchaService.verify(dto.getCaptchaId(), dto.getCaptchaCode());
        String configuredToken = resetToken == null ? "" : resetToken.trim();
        if (!StringUtils.hasText(configuredToken)) {
            throw new BizException(503, "找回密码未启用：请在服务端配置 ADMIN_RESET_TOKEN 后再试");
        }
        if (!matchesToken(configuredToken, dto.getResetToken())) {
            throw new BizException(403, "找回密码令牌不正确");
        }
        SysAdmin admin = sysAdminMapper.selectOne(new LambdaQueryWrapper<SysAdmin>().eq(SysAdmin::getUsername, dto.getUsername()));
        if (admin == null) throw new BizException(404, "管理员账号不存在");
        admin.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        sysAdminMapper.updateById(admin);
    }

    @Override
    public void logout() {
        StpAdminUtil.stpLogic.logout();
    }

    @Override
    public AdminInfoVO getCurrentAdminInfo() {
        Long adminId = StpAdminUtil.stpLogic.getLoginIdAsLong();
        SysAdmin admin = sysAdminMapper.selectById(adminId);
        if (admin == null) {
            throw BizException.notFound("管理员不存在");
        }
        AdminInfoVO vo = new AdminInfoVO();
        vo.setId(admin.getId());
        vo.setUsername(admin.getUsername());
        vo.setRealName(admin.getRealName());
        vo.setRole(admin.getRole());
        return vo;
    }

    /** 恒定时间比较，避免通过响应耗时逐字符试探恢复令牌。 */
    private boolean matchesToken(String configuredToken, String providedToken) {
        if (!StringUtils.hasText(providedToken)) {
            return false;
        }
        return MessageDigest.isEqual(configuredToken.getBytes(StandardCharsets.UTF_8),
                providedToken.trim().getBytes(StandardCharsets.UTF_8));
    }

    /** 无 Web 上下文（例如单元测试）时按未登录处理。 */
    private boolean isAdminLoggedIn() {
        try {
            return StpAdminUtil.stpLogic.isLogin();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void writeLoginSuccessLog(String username, String ip, String userAgent) {        writeLoginLog(username, ip, userAgent, 1, null);
        writeSysLog(username, ip, 1, null);
    }

    private void writeLoginFailureLog(String username, String ip, String userAgent, String reason) {
        writeLoginLog(username, ip, userAgent, 0, reason);
        writeSysLog(username, ip, 0, reason);
    }

    private void writeLoginLog(String username, String ip, String userAgent, int status, String msg) {
        SysLoginLogEntity logEntity = new SysLoginLogEntity();
        logEntity.setUsername(username);
        logEntity.setIp(ip);
        logEntity.setUserAgent(userAgent);
        logEntity.setStatus(status);
        logEntity.setMsg(msg);
        sysLoginLogMapper.insert(logEntity);
    }

    private void writeSysLog(String username, String ip, int success, String errorMsg) {
        SysLogEntity sysLogEntity = new SysLogEntity();
        sysLogEntity.setLogType("LOGIN");
        sysLogEntity.setBizDesc("管理员登录");
        sysLogEntity.setOperator(username);
        sysLogEntity.setIp(ip);
        sysLogEntity.setSuccess(success);
        sysLogEntity.setErrorMsg(errorMsg);
        sysLogMapper.insert(sysLogEntity);
    }
}
