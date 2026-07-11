package com.guido.scenicai.module.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guido.scenicai.common.config.StpTouristUtil;
import com.guido.scenicai.module.chat.service.TouristRouteRecommendService;
import com.guido.scenicai.module.chat.vo.RouteRecommendSpotVO;
import com.guido.scenicai.module.chat.vo.RouteRecommendVO;
import com.guido.scenicai.module.route.entity.Route;
import com.guido.scenicai.module.route.entity.RouteSpot;
import com.guido.scenicai.module.route.mapper.RouteMapper;
import com.guido.scenicai.module.route.mapper.RouteSpotMapper;
import com.guido.scenicai.module.spot.entity.Spot;
import com.guido.scenicai.module.spot.mapper.SpotMapper;
import com.guido.scenicai.module.tourist.entity.TouristUser;
import com.guido.scenicai.module.tourist.mapper.TouristUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TouristRouteRecommendServiceImpl implements TouristRouteRecommendService {

    private final RouteMapper routeMapper;
    private final RouteSpotMapper routeSpotMapper;
    private final SpotMapper spotMapper;
    private final TouristUserMapper touristUserMapper;

    @Override
    public List<RouteRecommendVO> recommend(Long scenicId, String interest, Integer routeType) {
        String effectiveInterest = StringUtils.hasText(interest) ? interest : currentUserInterest();
        List<String> keywords = preferenceKeywords(effectiveInterest);
        List<Route> routes = routeMapper.selectList(new LambdaQueryWrapper<Route>()
                .eq(Route::getScenicId, scenicId)
                .eq(Route::getStatus, 1)
                .eq(routeType != null, Route::getType, routeType));
        return routes.stream()
                .sorted(Comparator.comparingInt(route -> -score(route, keywords)))
                .limit(5)
                .map(route -> toVO(route, effectiveInterest, keywords))
                .toList();
    }

    private String currentUserInterest() {
        Long touristId = StpTouristUtil.stpLogic.getLoginIdAsLong();
        TouristUser user = touristUserMapper.selectById(touristId);
        return user == null ? null : user.getInterestTags();
    }

    private int score(Route route, List<String> keywords) {
        int value = 0;
        if (keywords.isEmpty()) {
            return value;
        }
        for (String keyword : keywords) {
            value += contains(route.getInterestTags(), keyword) ? 5 : 0;
            value += contains(route.getSuitCrowd(), keyword) ? 3 : 0;
            value += contains(route.getName(), keyword) ? 2 : 0;
            value += contains(route.getIntro(), keyword) ? 1 : 0;
            value += typeScore(route.getType(), keyword);
        }
        return value;
    }

    private int typeScore(Integer type, String keyword) {
        if (type == null) {
            return 0;
        }
        return switch (type) {
            case 1 -> containsAny(keyword, "经典", "首次", "第一次") ? 2 : 0;
            case 2 -> containsAny(keyword, "亲子", "孩子", "家庭") ? 2 : 0;
            case 3 -> containsAny(keyword, "老人", "长者", "轻松") ? 2 : 0;
            case 4 -> containsAny(keyword, "历史", "文化", "古迹") ? 2 : 0;
            case 5 -> containsAny(keyword, "摄影", "拍照", "打卡") ? 2 : 0;
            case 6 -> containsAny(keyword, "轻松", "休闲") ? 2 : 0;
            case 7 -> containsAny(keyword, "深度", "完整") ? 2 : 0;
            default -> 0;
        };
    }

    private boolean contains(String source, String keyword) {
        if (!StringUtils.hasText(source) || !StringUtils.hasText(keyword)) {
            return false;
        }
        return source.contains(keyword) || keyword.contains(source);
    }

    private boolean containsAny(String source, String... words) {
        if (!StringUtils.hasText(source)) {
            return false;
        }
        for (String word : words) {
            if (source.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private RouteRecommendVO toVO(Route route, String interest, List<String> keywords) {
        RouteRecommendVO vo = new RouteRecommendVO();
        vo.setRouteId(route.getId());
        vo.setName(route.getName());
        vo.setType(route.getType());
        vo.setEstimateMinutes(route.getEstimateMinutes());
        vo.setRecommendReason(reason(route, interest, keywords));
        vo.setSpots(routeSpots(route.getId()));
        return vo;
    }

    private String reason(Route route, String interest, List<String> keywords) {
        if (StringUtils.hasText(route.getRecommendReason())) {
            return route.getRecommendReason();
        }
        if (!keywords.isEmpty()) {
            return "适合对" + String.join("、", keywords) + "感兴趣的游客";
        }
        return "根据当前景区热门路线推荐";
    }

    private List<String> preferenceKeywords(String interest) {
        if (!StringUtils.hasText(interest)) {
            return List.of();
        }
        Set<String> keywords = new LinkedHashSet<>();
        for (String word : interest.split("[,，、\\s;；。.!！?？/]+")) {
            if (StringUtils.hasText(word)) {
                keywords.add(word.trim());
            }
        }
        addIfContains(interest, keywords, "历史", "文化", "古迹", "文物");
        addIfContains(interest, keywords, "自然", "山水", "风光", "生态");
        addIfContains(interest, keywords, "亲子", "孩子", "家庭");
        addIfContains(interest, keywords, "摄影", "拍照", "打卡");
        addIfContains(interest, keywords, "老人", "长者", "轻松", "休闲");
        addIfContains(interest, keywords, "经典", "首次", "第一次");
        addIfContains(interest, keywords, "深度", "完整");
        List<String> result = keywords.stream()
                .filter(word -> word.length() <= 12)
                .toList();
        List<String> compact = result.stream()
                .filter(word -> !containsShorterKeyword(word, result))
                .toList();
        return compact.isEmpty() ? result : compact;
    }

    private void addIfContains(String interest, Set<String> keywords, String... candidates) {
        for (String candidate : candidates) {
            if (interest.contains(candidate)) {
                keywords.add(candidate);
            }
        }
    }

    private boolean containsShorterKeyword(String word, List<String> keywords) {
        if (word.length() <= 4) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword.length() < word.length() && word.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private List<RouteRecommendSpotVO> routeSpots(Long routeId) {
        List<RouteSpot> rows = routeSpotMapper.selectList(new LambdaQueryWrapper<RouteSpot>()
                .eq(RouteSpot::getRouteId, routeId)
                .orderByAsc(RouteSpot::getSortOrder));
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, String> spotNameMap = spotMapper.selectBatchIds(rows.stream()
                        .map(RouteSpot::getSpotId).toList())
                .stream()
                .collect(Collectors.toMap(Spot::getId, Spot::getName));
        return rows.stream().map(row -> {
            RouteRecommendSpotVO vo = new RouteRecommendSpotVO();
            vo.setSpotId(row.getSpotId());
            vo.setName(spotNameMap.get(row.getSpotId()));
            vo.setSortOrder(row.getSortOrder());
            return vo;
        }).toList();
    }
}
