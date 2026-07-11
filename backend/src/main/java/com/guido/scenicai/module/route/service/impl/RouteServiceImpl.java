package com.guido.scenicai.module.route.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.common.result.PageResult;
import com.guido.scenicai.module.route.dto.RoutePageQueryDTO;
import com.guido.scenicai.module.route.dto.RouteSaveDTO;
import com.guido.scenicai.module.route.dto.RouteSpotsDTO;
import com.guido.scenicai.module.route.dto.RouteStatusDTO;
import com.guido.scenicai.module.route.dto.RouteUpdateDTO;
import com.guido.scenicai.module.route.entity.Route;
import com.guido.scenicai.module.route.entity.RouteSpot;
import com.guido.scenicai.module.route.mapper.RouteMapper;
import com.guido.scenicai.module.route.mapper.RouteSpotMapper;
import com.guido.scenicai.module.route.service.RouteService;
import com.guido.scenicai.module.route.vo.RouteSpotVO;
import com.guido.scenicai.module.route.vo.RouteVO;
import com.guido.scenicai.module.scenic.entity.Scenic;
import com.guido.scenicai.module.scenic.mapper.ScenicMapper;
import com.guido.scenicai.module.spot.entity.Spot;
import com.guido.scenicai.module.spot.mapper.SpotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private final RouteMapper routeMapper;
    private final RouteSpotMapper routeSpotMapper;
    private final SpotMapper spotMapper;
    private final ScenicMapper scenicMapper;

    @Override
    public PageResult<RouteVO> pageQuery(RoutePageQueryDTO query) {
        LambdaQueryWrapper<Route> wrapper = new LambdaQueryWrapper<>();
        if (query.getScenicId() != null) {
            wrapper.eq(Route::getScenicId, query.getScenicId());
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
            wrapper.in(Route::getScenicId, scenicIds);
        }
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(Route::getName, query.getName());
        }
        if (query.getType() != null) {
            wrapper.eq(Route::getType, query.getType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Route::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Route::getId);

        Page<Route> page = routeMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );
        Map<Long, String> scenicNameMap = loadScenicNameMap(page.getRecords());
        List<RouteVO> records = page.getRecords().stream()
                .map(route -> toVO(route, false, scenicNameMap))
                .toList();
        return PageResult.of(page.getTotal(), query.getPageNum(), query.getPageSize(), records);
    }

    @Override
    public RouteVO getDetail(Long id) {
        return toVO(getRequired(id), true);
    }

    @Override
    @Transactional
    public RouteVO create(RouteSaveDTO dto) {
        Route route = new Route();
        copySaveFields(dto, route);
        if (route.getStatus() == null) {
            route.setStatus(1);
        }
        routeMapper.insert(route);
        return toVO(route, false);
    }

    @Override
    @Transactional
    public RouteVO modify(RouteUpdateDTO dto) {
        Route route = getRequired(dto.getId());
        copySaveFields(dto, route);
        routeMapper.updateById(route);
        return toVO(route, true);
    }

    @Override
    @Transactional
    public void remove(Long id) {
        getRequired(id);
        routeSpotMapper.delete(new LambdaQueryWrapper<RouteSpot>().eq(RouteSpot::getRouteId, id));
        routeMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void changeStatus(RouteStatusDTO dto) {
        Route route = getRequired(dto.getId());
        route.setStatus(dto.getStatus());
        routeMapper.updateById(route);
    }

    @Override
    @Transactional
    public void configureSpots(RouteSpotsDTO dto) {
        getRequired(dto.getRouteId());
        routeSpotMapper.delete(new LambdaQueryWrapper<RouteSpot>()
                .eq(RouteSpot::getRouteId, dto.getRouteId()));
        for (int i = 0; i < dto.getSpotIds().size(); i++) {
            Long spotId = dto.getSpotIds().get(i);
            if (spotMapper.selectById(spotId) == null) {
                throw BizException.notFound("景点不存在：" + spotId);
            }
            RouteSpot routeSpot = new RouteSpot();
            routeSpot.setRouteId(dto.getRouteId());
            routeSpot.setSpotId(spotId);
            routeSpot.setSortOrder(i + 1);
            routeSpotMapper.insert(routeSpot);
        }
    }

    private Route getRequired(Long id) {
        Route route = routeMapper.selectById(id);
        if (route == null) {
            throw BizException.notFound("路线不存在");
        }
        return route;
    }

    private void copySaveFields(RouteSaveDTO dto, Route route) {
        route.setScenicId(dto.getScenicId());
        route.setName(dto.getName());
        route.setType(dto.getType());
        route.setIntro(dto.getIntro());
        route.setEstimateMinutes(dto.getEstimateMinutes());
        route.setSuitCrowd(dto.getSuitCrowd());
        route.setInterestTags(dto.getInterestTags());
        route.setRecommendReason(dto.getRecommendReason());
        route.setNotice(dto.getNotice());
        route.setStatus(dto.getStatus());
    }

    private RouteVO toVO(Route route, boolean includeSpots) {
        return toVO(route, includeSpots, Map.of());
    }

    private RouteVO toVO(Route route, boolean includeSpots, Map<Long, String> scenicNameMap) {
        RouteVO vo = new RouteVO();
        vo.setId(route.getId());
        vo.setScenicId(route.getScenicId());
        vo.setScenicName(resolveScenicName(route.getScenicId(), scenicNameMap));
        vo.setName(route.getName());
        vo.setType(route.getType());
        vo.setIntro(route.getIntro());
        vo.setEstimateMinutes(route.getEstimateMinutes());
        vo.setSuitCrowd(route.getSuitCrowd());
        vo.setInterestTags(route.getInterestTags());
        vo.setRecommendReason(route.getRecommendReason());
        vo.setNotice(route.getNotice());
        vo.setStatus(route.getStatus());
        vo.setCreateTime(route.getCreateTime());
        vo.setUpdateTime(route.getUpdateTime());
        if (includeSpots) {
            vo.setSpots(getRouteSpots(route.getId()));
        }
        return vo;
    }

    private Map<Long, String> loadScenicNameMap(List<Route> routes) {
        List<Long> scenicIds = routes.stream()
                .map(Route::getScenicId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (scenicIds.isEmpty()) {
            return Map.of();
        }
        return scenicMapper.selectBatchIds(scenicIds).stream()
                .collect(Collectors.toMap(Scenic::getId, Scenic::getName, (left, right) -> left));
    }

    private String resolveScenicName(Long scenicId, Map<Long, String> scenicNameMap) {
        if (scenicId == null) {
            return null;
        }
        String scenicName = scenicNameMap.get(scenicId);
        if (scenicName != null) {
            return scenicName;
        }
        Scenic scenic = scenicMapper.selectById(scenicId);
        return scenic == null ? null : scenic.getName();
    }

    private List<RouteSpotVO> getRouteSpots(Long routeId) {
        List<RouteSpot> rows = routeSpotMapper.selectList(new LambdaQueryWrapper<RouteSpot>()
                .eq(RouteSpot::getRouteId, routeId)
                .orderByAsc(RouteSpot::getSortOrder));
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> spotIds = rows.stream().map(RouteSpot::getSpotId).toList();
        Map<Long, String> spotNameMap = spotMapper.selectBatchIds(spotIds).stream()
                .collect(Collectors.toMap(Spot::getId, Spot::getName));
        return rows.stream().map(row -> {
            RouteSpotVO vo = new RouteSpotVO();
            vo.setSpotId(row.getSpotId());
            vo.setName(spotNameMap.get(row.getSpotId()));
            vo.setSortOrder(row.getSortOrder());
            return vo;
        }).toList();
    }
}
