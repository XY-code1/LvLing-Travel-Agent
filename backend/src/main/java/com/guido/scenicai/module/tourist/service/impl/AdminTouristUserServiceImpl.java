package com.guido.scenicai.module.tourist.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.common.result.PageResult;
import com.guido.scenicai.module.tourist.dto.AdminTouristUserPageQueryDTO;
import com.guido.scenicai.module.tourist.dto.TouristUserStatusDTO;
import com.guido.scenicai.module.tourist.entity.TouristUser;
import com.guido.scenicai.module.tourist.mapper.TouristUserMapper;
import com.guido.scenicai.module.tourist.service.AdminTouristUserService;
import com.guido.scenicai.module.tourist.vo.AdminTouristUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminTouristUserServiceImpl implements AdminTouristUserService {

    private final TouristUserMapper touristUserMapper;

    @Override
    public PageResult<AdminTouristUserVO> page(AdminTouristUserPageQueryDTO query) {
        LambdaQueryWrapper<TouristUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getPhone())) {
            wrapper.like(TouristUser::getPhone, query.getPhone());
        }
        if (StringUtils.hasText(query.getNickname())) {
            wrapper.like(TouristUser::getNickname, query.getNickname());
        }
        if (query.getStatus() != null) {
            wrapper.eq(TouristUser::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(TouristUser::getId);
        Page<TouristUser> page = touristUserMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<AdminTouristUserVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page.getTotal(), query.getPageNum(), query.getPageSize(), records);
    }

    @Override
    @Transactional
    public void changeStatus(TouristUserStatusDTO dto) {
        TouristUser user = touristUserMapper.selectById(dto.getId());
        if (user == null) {
            throw BizException.notFound("游客用户不存在");
        }
        user.setStatus(dto.getStatus());
        touristUserMapper.updateById(user);
    }

    private AdminTouristUserVO toVO(TouristUser user) {
        AdminTouristUserVO vo = new AdminTouristUserVO();
        vo.setId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setInterestTags(user.getInterestTags());
        vo.setStatus(user.getStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        return vo;
    }
}
