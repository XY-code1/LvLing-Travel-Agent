package com.guido.scenicai.module.feature.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.common.result.PageResult;
import com.guido.scenicai.module.feature.dto.FeatureItemPageQueryDTO;
import com.guido.scenicai.module.feature.dto.FeatureItemSaveDTO;
import com.guido.scenicai.module.feature.dto.FeatureItemStatusDTO;
import com.guido.scenicai.module.feature.dto.FeatureItemUpdateDTO;
import com.guido.scenicai.module.feature.entity.AdminFeatureItem;
import com.guido.scenicai.module.feature.mapper.AdminFeatureItemMapper;
import com.guido.scenicai.module.feature.service.AdminFeatureItemService;
import com.guido.scenicai.module.feature.vo.FeatureItemVO;
import com.guido.scenicai.module.route.entity.Route;
import com.guido.scenicai.module.route.mapper.RouteMapper;
import com.guido.scenicai.module.scenic.entity.Scenic;
import com.guido.scenicai.module.scenic.mapper.ScenicMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminFeatureItemServiceImpl implements AdminFeatureItemService {

    private final AdminFeatureItemMapper featureItemMapper;
    private final ScenicMapper scenicMapper;
    private final RouteMapper routeMapper;

    @Override
    public PageResult<FeatureItemVO> page(FeatureItemPageQueryDTO query) {
        LambdaQueryWrapper<AdminFeatureItem> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getModuleType())) {
            wrapper.eq(AdminFeatureItem::getModuleType, query.getModuleType());
        }
        if (StringUtils.hasText(query.getCategory())) {
            wrapper.like(AdminFeatureItem::getCategory, query.getCategory());
        }
        if (query.getStatus() != null) {
            wrapper.eq(AdminFeatureItem::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(AdminFeatureItem::getTitle, query.getKeyword())
                    .or()
                    .like(AdminFeatureItem::getContent, query.getKeyword()));
        }
        if (StringUtils.hasText(query.getScenicName())) {
            List<Long> scenicIds = scenicMapper.selectList(new LambdaQueryWrapper<Scenic>()
                            .like(Scenic::getName, query.getScenicName()))
                    .stream()
                    .map(Scenic::getId)
                    .toList();
            if (scenicIds.isEmpty()) {
                return PageResult.empty(query.getPageNum(), query.getPageSize());
            }
            wrapper.in(AdminFeatureItem::getScenicId, scenicIds);
        }
        wrapper.orderByAsc(AdminFeatureItem::getSortOrder).orderByDesc(AdminFeatureItem::getId);
        Page<AdminFeatureItem> page = featureItemMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        Map<Long, String> scenicNameMap = loadScenicNameMap(page.getRecords());
        Map<Long, String> routeNameMap = loadRouteNameMap(page.getRecords());
        List<FeatureItemVO> records = page.getRecords().stream()
                .map(item -> toVO(item, scenicNameMap, routeNameMap))
                .toList();
        return PageResult.of(page.getTotal(), query.getPageNum(), query.getPageSize(), records);
    }

    @Override
    @Transactional
    public FeatureItemVO create(FeatureItemSaveDTO dto) {
        AdminFeatureItem item = new AdminFeatureItem();
        copyFields(dto, item);
        if (item.getStatus() == null) {
            item.setStatus(1);
        }
        if (item.getSortOrder() == null) {
            item.setSortOrder(0);
        }
        featureItemMapper.insert(item);
        return toVO(item, Map.of(), Map.of());
    }

    @Override
    @Transactional
    public FeatureItemVO modify(FeatureItemUpdateDTO dto) {
        AdminFeatureItem item = getRequired(dto.getId());
        copyFields(dto, item);
        featureItemMapper.updateById(item);
        return toVO(item, Map.of(), Map.of());
    }

    @Override
    @Transactional
    public void remove(Long id) {
        getRequired(id);
        featureItemMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void changeStatus(FeatureItemStatusDTO dto) {
        AdminFeatureItem item = getRequired(dto.getId());
        item.setStatus(dto.getStatus());
        featureItemMapper.updateById(item);
    }

    private AdminFeatureItem getRequired(Long id) {
        AdminFeatureItem item = featureItemMapper.selectById(id);
        if (item == null) {
            throw BizException.notFound("运营配置不存在");
        }
        return item;
    }

    private void copyFields(FeatureItemSaveDTO dto, AdminFeatureItem item) {
        item.setModuleType(dto.getModuleType());
        item.setScenicId(dto.getScenicId());
        item.setRelatedId(dto.getRelatedId());
        item.setTitle(dto.getTitle());
        item.setCategory(dto.getCategory());
        item.setContent(dto.getContent());
        item.setMediaUrl(dto.getMediaUrl());
        item.setLongitude(dto.getLongitude());
        item.setLatitude(dto.getLatitude());
        item.setSortOrder(dto.getSortOrder());
        item.setStatus(dto.getStatus());
        item.setRemark(dto.getRemark());
    }

    private Map<Long, String> loadScenicNameMap(List<AdminFeatureItem> items) {
        List<Long> scenicIds = items.stream()
                .map(AdminFeatureItem::getScenicId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (scenicIds.isEmpty()) {
            return Map.of();
        }
        return scenicMapper.selectBatchIds(scenicIds).stream()
                .collect(Collectors.toMap(Scenic::getId, Scenic::getName, (left, right) -> left));
    }

    private Map<Long, String> loadRouteNameMap(List<AdminFeatureItem> items) {
        List<Long> routeIds = items.stream()
                .filter(item -> "ROUTE_CONTENT".equals(item.getModuleType()))
                .map(AdminFeatureItem::getRelatedId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (routeIds.isEmpty()) {
            return Map.of();
        }
        return routeMapper.selectBatchIds(routeIds).stream()
                .collect(Collectors.toMap(Route::getId, Route::getName, (left, right) -> left));
    }

    private FeatureItemVO toVO(AdminFeatureItem item, Map<Long, String> scenicNameMap, Map<Long, String> routeNameMap) {
        FeatureItemVO vo = new FeatureItemVO();
        vo.setId(item.getId());
        vo.setModuleType(item.getModuleType());
        vo.setScenicId(item.getScenicId());
        vo.setScenicName(resolveScenicName(item.getScenicId(), scenicNameMap));
        vo.setRelatedId(item.getRelatedId());
        vo.setRelatedName(resolveRelatedName(item, routeNameMap));
        vo.setTitle(item.getTitle());
        vo.setCategory(item.getCategory());
        vo.setContent(item.getContent());
        vo.setMediaUrl(item.getMediaUrl());
        vo.setLongitude(item.getLongitude());
        vo.setLatitude(item.getLatitude());
        vo.setSortOrder(item.getSortOrder());
        vo.setStatus(item.getStatus());
        vo.setRemark(item.getRemark());
        vo.setCreateTime(item.getCreateTime());
        vo.setUpdateTime(item.getUpdateTime());
        return vo;
    }

    private String resolveScenicName(Long scenicId, Map<Long, String> scenicNameMap) {
        if (scenicId == null) {
            return null;
        }
        String name = scenicNameMap.get(scenicId);
        if (name != null) {
            return name;
        }
        Scenic scenic = scenicMapper.selectById(scenicId);
        return scenic == null ? null : scenic.getName();
    }

    private String resolveRelatedName(AdminFeatureItem item, Map<Long, String> routeNameMap) {
        if (!"ROUTE_CONTENT".equals(item.getModuleType()) || item.getRelatedId() == null) {
            return null;
        }
        String name = routeNameMap.get(item.getRelatedId());
        if (name != null) {
            return name;
        }
        Route route = routeMapper.selectById(item.getRelatedId());
        return route == null ? null : route.getName();
    }
}
