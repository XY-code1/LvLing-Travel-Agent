package com.guido.scenicai.module.tourist.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guido.scenicai.common.config.StpTouristUtil;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.module.tourist.dto.TouristLoginDTO;
import com.guido.scenicai.module.tourist.dto.TouristRegisterDTO;
import com.guido.scenicai.module.tourist.entity.TouristUser;
import com.guido.scenicai.module.tourist.mapper.TouristUserMapper;
import com.guido.scenicai.module.tourist.service.TouristAuthService;
import com.guido.scenicai.module.tourist.vo.TouristInfoVO;
import com.guido.scenicai.module.tourist.vo.TouristLoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TouristAuthServiceImpl implements TouristAuthService {

    private final TouristUserMapper touristUserMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Long register(TouristRegisterDTO dto) {
        // 检查手机号是否已注册
        Long count = touristUserMapper.selectCount(
                new LambdaQueryWrapper<TouristUser>()
                        .eq(TouristUser::getPhone, dto.getPhone())
        );
        if (count > 0) {
            throw new BizException(400, "该手机号已注册");
        }

        TouristUser user = new TouristUser();
        user.setPhone(dto.getPhone());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : "游客" + dto.getPhone().substring(7));
        user.setGender(0);
        user.setStatus(1);

        touristUserMapper.insert(user);
        log.info("游客注册成功: phone={}, id={}", dto.getPhone(), user.getId());
        return user.getId();
    }

    @Override
    @Transactional
    public TouristLoginVO login(TouristLoginDTO dto) {
        TouristUser user = touristUserMapper.selectOne(
                new LambdaQueryWrapper<TouristUser>()
                        .eq(TouristUser::getPhone, dto.getPhone())
        );
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(400, "账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(403, "账号已被封禁");
        }

        // Sa-Token tourist 登录
        StpTouristUtil.stpLogic.login(user.getId());

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        touristUserMapper.updateById(user);

        String token = StpTouristUtil.stpLogic.getTokenValue();

        TouristInfoVO infoVO = buildTouristInfo(user);

        log.info("游客登录成功: phone={}, id={}", user.getPhone(), user.getId());
        return new TouristLoginVO(token, infoVO);
    }

    @Override
    public void logout() {
        StpTouristUtil.stpLogic.logout();
    }

    /**
     * 构建游客简要信息，手机号中间 4 位脱敏。
     */
    private TouristInfoVO buildTouristInfo(TouristUser user) {
        TouristInfoVO vo = new TouristInfoVO();
        vo.setId(user.getId());
        vo.setPhone(maskPhone(user.getPhone()));
        vo.setNickname(user.getNickname());
        vo.setInterestTags(user.getInterestTags());
        return vo;
    }

    /**
     * 手机号脱敏：138****0000。
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
