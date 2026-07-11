package com.guido.scenicai.module.scenic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.common.result.PageResult;
import com.guido.scenicai.module.scenic.dto.ScenicPageQueryDTO;
import com.guido.scenicai.module.scenic.dto.ScenicSaveDTO;
import com.guido.scenicai.module.scenic.dto.ScenicStatusDTO;
import com.guido.scenicai.module.scenic.dto.ScenicUpdateDTO;
import com.guido.scenicai.module.scenic.entity.Scenic;
import com.guido.scenicai.module.scenic.mapper.ScenicMapper;
import com.guido.scenicai.module.scenic.service.ScenicService;
import com.guido.scenicai.module.scenic.vo.ScenicVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScenicServiceImpl implements ScenicService {

    private final ScenicMapper scenicMapper;

    @Override
    public PageResult<ScenicVO> pageQuery(ScenicPageQueryDTO query) {
        LambdaQueryWrapper<Scenic> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(Scenic::getName, query.getName());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Scenic::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Scenic::getId);

        Page<Scenic> page = scenicMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );
        List<ScenicVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page.getTotal(), query.getPageNum(), query.getPageSize(), records);
    }

    @Override
    public ScenicVO getDetail(Long id) {
        return toVO(getRequired(id));
    }

    @Override
    @Transactional
    public ScenicVO create(ScenicSaveDTO dto) {
        Scenic scenic = new Scenic();
        copySaveFields(dto, scenic);
        if (scenic.getStatus() == null) {
            scenic.setStatus(1);
        }
        scenicMapper.insert(scenic);
        return toVO(scenic);
    }

    @Override
    @Transactional
    public ScenicVO modify(ScenicUpdateDTO dto) {
        Scenic scenic = getRequired(dto.getId());
        copySaveFields(dto, scenic);
        scenicMapper.updateById(scenic);
        return toVO(scenic);
    }

    @Override
    @Transactional
    public void remove(Long id) {
        getRequired(id);
        scenicMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void changeStatus(ScenicStatusDTO dto) {
        Scenic scenic = getRequired(dto.getId());
        scenic.setStatus(dto.getStatus());
        scenicMapper.updateById(scenic);
    }

    private Scenic getRequired(Long id) {
        Scenic scenic = scenicMapper.selectById(id);
        if (scenic == null) {
            throw BizException.notFound("景区不存在");
        }
        return scenic;
    }

    private void copySaveFields(ScenicSaveDTO dto, Scenic scenic) {
        scenic.setName(dto.getName());
        scenic.setIntro(dto.getIntro());
        scenic.setAddress(dto.getAddress());
        scenic.setOpenTime(dto.getOpenTime());
        scenic.setTicketInfo(dto.getTicketInfo());
        scenic.setTrafficInfo(dto.getTrafficInfo());
        scenic.setServicePhone(dto.getServicePhone());
        scenic.setNotice(dto.getNotice());
        scenic.setCoverImage(dto.getCoverImage());
        scenic.setLongitude(dto.getLongitude());
        scenic.setLatitude(dto.getLatitude());
        scenic.setStatus(dto.getStatus());
    }

    private ScenicVO toVO(Scenic scenic) {
        ScenicVO vo = new ScenicVO();
        vo.setId(scenic.getId());
        vo.setName(scenic.getName());
        vo.setIntro(scenic.getIntro());
        vo.setAddress(scenic.getAddress());
        vo.setOpenTime(scenic.getOpenTime());
        vo.setTicketInfo(scenic.getTicketInfo());
        vo.setTrafficInfo(scenic.getTrafficInfo());
        vo.setServicePhone(scenic.getServicePhone());
        vo.setNotice(scenic.getNotice());
        vo.setCoverImage(scenic.getCoverImage());
        vo.setLongitude(scenic.getLongitude());
        vo.setLatitude(scenic.getLatitude());
        vo.setStatus(scenic.getStatus());
        vo.setCreateTime(scenic.getCreateTime());
        vo.setUpdateTime(scenic.getUpdateTime());
        return vo;
    }
}
