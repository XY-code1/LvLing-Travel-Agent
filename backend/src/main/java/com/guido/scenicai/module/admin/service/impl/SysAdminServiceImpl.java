package com.guido.scenicai.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guido.scenicai.common.config.StpAdminUtil;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.module.admin.dto.AdminLoginDTO;
import com.guido.scenicai.module.admin.entity.SysAdmin;
import com.guido.scenicai.module.admin.entity.SysLoginLogEntity;
import com.guido.scenicai.module.admin.mapper.SysAdminMapper;
import com.guido.scenicai.module.admin.mapper.SysLoginLogMapper;
import com.guido.scenicai.module.admin.service.SysAdminService;
import com.guido.scenicai.module.admin.vo.AdminInfoVO;
import com.guido.scenicai.module.admin.vo.AdminLoginVO;
import com.guido.scenicai.module.log.entity.SysLogEntity;
import com.guido.scenicai.module.log.mapper.SysLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysAdminServiceImpl implements SysAdminService {

    private final SysAdminMapper sysAdminMapper;
    private final SysLogMapper sysLogMapper;
    private final SysLoginLogMapper sysLoginLogMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public AdminLoginVO login(AdminLoginDTO dto, String ip, String userAgent) {
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

    private void writeLoginSuccessLog(String username, String ip, String userAgent) {
        writeLoginLog(username, ip, userAgent, 1, null);
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
