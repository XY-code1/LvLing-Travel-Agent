package com.guido.scenicai.module.spot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guido.scenicai.common.exception.BizException;
import com.guido.scenicai.common.result.PageResult;
import com.guido.scenicai.module.spot.dto.SpotPageQueryDTO;
import com.guido.scenicai.module.spot.dto.SpotSaveDTO;
import com.guido.scenicai.module.spot.dto.SpotStatusDTO;
import com.guido.scenicai.module.spot.dto.SpotUpdateDTO;
import com.guido.scenicai.module.spot.entity.Spot;
import com.guido.scenicai.module.spot.mapper.SpotMapper;
import com.guido.scenicai.module.scenic.entity.Scenic;
import com.guido.scenicai.module.scenic.mapper.ScenicMapper;
import com.guido.scenicai.module.spot.service.SpotService;
import com.guido.scenicai.module.spot.vo.HotSpotVO;
import com.guido.scenicai.module.spot.vo.NearbySpotVO;
import com.guido.scenicai.module.spot.vo.SpotVO;
import com.guido.scenicai.module.spot.vo.TouristHomeHotVO;
import com.guido.scenicai.module.spot.vo.TouristSpotDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpotServiceImpl implements SpotService {

    private final SpotMapper spotMapper;
    private final ScenicMapper scenicMapper;

    @Override
    public PageResult<SpotVO> pageQuery(SpotPageQueryDTO query) {
        LambdaQueryWrapper<Spot> wrapper = new LambdaQueryWrapper<>();
        if (query.getScenicId() != null) {
            wrapper.eq(Spot::getScenicId, query.getScenicId());
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
            wrapper.in(Spot::getScenicId, scenicIds);
        }
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(Spot::getName, query.getName());
        }
        if (StringUtils.hasText(query.getTag())) {
            wrapper.like(Spot::getTags, query.getTag());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Spot::getStatus, query.getStatus());
        }
        if (query.getIsHot() != null) {
            wrapper.eq(Spot::getIsHot, query.getIsHot());
        }
        wrapper.orderByDesc(Spot::getIsHot).orderByDesc(Spot::getId);

        Page<Spot> page = spotMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );
        Map<Long, String> scenicNameMap = loadScenicNameMap(page.getRecords());
        List<SpotVO> records = page.getRecords().stream()
                .map(spot -> toVO(spot, scenicNameMap))
                .toList();
        return PageResult.of(page.getTotal(), query.getPageNum(), query.getPageSize(), records);
    }

    @Override
    public SpotVO getDetail(Long id) {
        return toVO(getRequired(id));
    }

    @Override
    public TouristSpotDetailVO getTouristDetail(Long id) {
        Spot spot = getRequired(id);
        if (!Integer.valueOf(1).equals(spot.getStatus())) {
            throw BizException.notFound("景点不存在或未启用");
        }
        TouristSpotDetailVO vo = toTouristDetailVO(spot);
        vo.setCanGuide(StringUtils.hasText(spot.getGuideText()));
        return vo;
    }

    @Override
    public TouristHomeHotVO hotForTourist() {
        Page<Spot> page = spotMapper.selectPage(
                new Page<>(1, 10),
                new LambdaQueryWrapper<Spot>()
                        .eq(Spot::getStatus, 1)
                        .eq(Spot::getIsHot, 1)
                        .orderByDesc(Spot::getId)
        );
        List<HotSpotVO> hotSpots = page.getRecords().stream()
                .map(this::toHotSpotVO)
                .toList();
        TouristHomeHotVO vo = new TouristHomeHotVO();
        vo.setHotSpots(hotSpots);
        vo.setRecommendQuestions(recommendQuestions(hotSpots));
        return vo;
    }

    @Override
    public List<NearbySpotVO> nearbyForTourist(Long scenicId, BigDecimal longitude, BigDecimal latitude) {
        List<Spot> spots = spotMapper.selectList(
                new LambdaQueryWrapper<Spot>()
                        .eq(Spot::getStatus, 1)
                        .eq(Spot::getScenicId, scenicId)
                        .isNotNull(Spot::getLongitude)
                        .isNotNull(Spot::getLatitude)
        );
        return spots.stream()
                .map(spot -> toNearbySpotVO(spot, longitude, latitude))
                .sorted(Comparator.comparing(NearbySpotVO::getDistanceMeters))
                .limit(8)
                .toList();
    }

    @Override
    @Transactional
    public SpotVO create(SpotSaveDTO dto) {
        Spot spot = new Spot();
        copySaveFields(dto, spot);
        if (spot.getStatus() == null) {
            spot.setStatus(1);
        }
        if (spot.getIsHot() == null) {
            spot.setIsHot(0);
        }
        spotMapper.insert(spot);
        return toVO(spot);
    }

    @Override
    @Transactional
    public SpotVO modify(SpotUpdateDTO dto) {
        Spot spot = getRequired(dto.getId());
        copySaveFields(dto, spot);
        spotMapper.updateById(spot);
        return toVO(spot);
    }

    @Override
    @Transactional
    public void remove(Long id) {
        getRequired(id);
        spotMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void changeStatus(SpotStatusDTO dto) {
        Spot spot = getRequired(dto.getId());
        spot.setStatus(dto.getStatus());
        spotMapper.updateById(spot);
    }

    private Spot getRequired(Long id) {
        Spot spot = spotMapper.selectById(id);
        if (spot == null) {
            throw BizException.notFound("景点不存在");
        }
        return spot;
    }

    private void copySaveFields(SpotSaveDTO dto, Spot spot) {
        spot.setScenicId(dto.getScenicId());
        spot.setName(dto.getName());
        spot.setAlias(dto.getAlias());
        spot.setIntro(dto.getIntro());
        spot.setHistoryCulture(dto.getHistoryCulture());
        spot.setGuideText(dto.getGuideText());
        spot.setTags(dto.getTags());
        spot.setImages(dto.getImages());
        spot.setStayMinutes(dto.getStayMinutes());
        spot.setSuitCrowd(dto.getSuitCrowd());
        spot.setIsHot(dto.getIsHot());
        spot.setLongitude(dto.getLongitude());
        spot.setLatitude(dto.getLatitude());
        spot.setStatus(dto.getStatus());
    }

    private SpotVO toVO(Spot spot) {
        return toVO(spot, Map.of());
    }

    private SpotVO toVO(Spot spot, Map<Long, String> scenicNameMap) {
        SpotVO vo = new SpotVO();
        fillVO(spot, vo, scenicNameMap);
        return vo;
    }

    private TouristSpotDetailVO toTouristDetailVO(Spot spot) {
        TouristSpotDetailVO vo = new TouristSpotDetailVO();
        fillVO(spot, vo, Map.of());
        return vo;
    }

    private void fillVO(Spot spot, SpotVO vo, Map<Long, String> scenicNameMap) {
        vo.setId(spot.getId());
        vo.setScenicId(spot.getScenicId());
        vo.setScenicName(resolveScenicName(spot.getScenicId(), scenicNameMap));
        vo.setName(spot.getName());
        vo.setAlias(spot.getAlias());
        vo.setIntro(spot.getIntro());
        vo.setHistoryCulture(spot.getHistoryCulture());
        vo.setGuideText(spot.getGuideText());
        vo.setTags(spot.getTags());
        vo.setImages(spot.getImages());
        vo.setStayMinutes(spot.getStayMinutes());
        vo.setSuitCrowd(spot.getSuitCrowd());
        vo.setIsHot(spot.getIsHot());
        vo.setLongitude(spot.getLongitude());
        vo.setLatitude(spot.getLatitude());
        vo.setStatus(spot.getStatus());
        vo.setCreateTime(spot.getCreateTime());
        vo.setUpdateTime(spot.getUpdateTime());
    }

    private Map<Long, String> loadScenicNameMap(List<Spot> spots) {
        List<Long> scenicIds = spots.stream()
                .map(Spot::getScenicId)
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

    private HotSpotVO toHotSpotVO(Spot spot) {
        HotSpotVO vo = new HotSpotVO();
        vo.setSpotId(spot.getId());
        vo.setName(spot.getName());
        vo.setCoverImage(firstImage(spot.getImages()));
        return vo;
    }

    private NearbySpotVO toNearbySpotVO(Spot spot, BigDecimal longitude, BigDecimal latitude) {
        NearbySpotVO vo = new NearbySpotVO();
        vo.setSpotId(spot.getId());
        vo.setName(spot.getName());
        vo.setCoverImage(firstImage(spot.getImages()));
        vo.setCanGuide(StringUtils.hasText(spot.getGuideText()));
        vo.setDistanceMeters(calculateDistanceMeters(latitude, longitude, spot.getLatitude(), spot.getLongitude()));
        return vo;
    }

    private long calculateDistanceMeters(
            BigDecimal fromLat,
            BigDecimal fromLon,
            BigDecimal toLat,
            BigDecimal toLon
    ) {
        double earthRadiusMeters = 6371000.0D;
        double lat1 = Math.toRadians(fromLat.doubleValue());
        double lat2 = Math.toRadians(toLat.doubleValue());
        double deltaLat = Math.toRadians(toLat.subtract(fromLat).doubleValue());
        double deltaLon = Math.toRadians(toLon.subtract(fromLon).doubleValue());
        double value = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double distance = earthRadiusMeters * 2 * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value));
        return BigDecimal.valueOf(distance).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private String firstImage(String images) {
        if (!StringUtils.hasText(images)) {
            return null;
        }
        String trimmed = images.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1)
                    .replace("\"", "")
                    .replace("'", "");
        }
        for (String item : trimmed.split("[,，]")) {
            if (StringUtils.hasText(item)) {
                return item.trim();
            }
        }
        return null;
    }

    private List<String> recommendQuestions(List<HotSpotVO> hotSpots) {
        List<String> questions = hotSpots.stream()
                .flatMap(spot -> List.of(
                        spot.getName() + "有什么历史故事？",
                        spot.getName() + "适合停留多久？"
                ).stream())
                .limit(6)
                .toList();
        if (!questions.isEmpty()) {
            return questions;
        }
        return List.of("景区有哪些必去景点？", "有什么适合我的游览路线？");
    }
}
