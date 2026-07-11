package com.guido.scenicai.module.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guido.scenicai.module.dashboard.dto.DemoSwitchDTO;
import com.guido.scenicai.module.dashboard.entity.DemoSwitch;
import com.guido.scenicai.module.dashboard.entity.StatDaily;
import com.guido.scenicai.module.dashboard.mapper.DemoSwitchMapper;
import com.guido.scenicai.module.dashboard.mapper.StatDailyMapper;
import com.guido.scenicai.module.dashboard.service.DashboardService;
import com.guido.scenicai.module.dashboard.vo.DashboardOverviewVO;
import com.guido.scenicai.module.dashboard.vo.TodayStatVO;
import com.guido.scenicai.module.dashboard.vo.TrendVO;
import com.guido.scenicai.module.tourist.entity.ChatMessage;
import com.guido.scenicai.module.tourist.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final String DASHBOARD_DEMO = "DASHBOARD_DEMO";

    private final StatDailyMapper statDailyMapper;
    private final DemoSwitchMapper demoSwitchMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ObjectMapper objectMapper;

    @Override
    public DashboardOverviewVO overview() {
        if (isDemoEnabled()) {
            return demoOverview();
        }
        LocalDate today = LocalDate.now();
        generateDailyStat(today);
        List<ChatMessage> todayMessages = loadMessages(today);
        DashboardOverviewVO vo = new DashboardOverviewVO();
        vo.setToday(toTodayStat(getStat(today)));
        vo.setHotQuestions(topQuestions(todayMessages));
        vo.setHotSpots(topSpots(todayMessages));
        vo.setEmotionDist(emotionDist(todayMessages));
        vo.setTrend7d(trend7d(today));
        vo.setDemo(0);
        return vo;
    }

    @Override
    @Transactional
    public void updateDemoSwitch(DemoSwitchDTO dto) {
        DemoSwitch entity = demoSwitchMapper.selectOne(new LambdaQueryWrapper<DemoSwitch>()
                .eq(DemoSwitch::getSwitchKey, dto.getSwitchKey())
                .last("LIMIT 1"));
        if (entity == null) {
            entity = new DemoSwitch();
            entity.setSwitchKey(dto.getSwitchKey());
            entity.setRemark("Created by dashboard switch API");
            entity.setEnabled(dto.getEnabled());
            demoSwitchMapper.insert(entity);
        } else {
            entity.setEnabled(dto.getEnabled());
            demoSwitchMapper.updateById(entity);
        }
    }

    @Override
    @Transactional
    public void generateDailyStat(LocalDate date) {
        List<ChatMessage> messages = loadMessages(date);
        StatDaily stat = getStat(date);
        if (stat == null) {
            stat = new StatDaily();
            stat.setStatDate(date);
        }
        fillStat(stat, messages);
        if (stat.getId() == null) {
            statDailyMapper.insert(stat);
        } else {
            statDailyMapper.updateById(stat);
        }
    }

    private void fillStat(StatDaily stat, List<ChatMessage> messages) {
        int total = messages.size();
        stat.setServiceCount(total);
        stat.setQaCount(total);
        stat.setVoiceCount(countInput(messages, "VOICE"));
        stat.setImageCount(countInput(messages, "IMAGE"));
        stat.setAvatarCount(Math.toIntExact(messages.stream()
                .filter(message -> StringUtils.hasText(message.getStreamUrl())).count()));
        stat.setAvgCostMs(avgCost(messages));
        stat.setKbHitRate(kbHitRate(messages));
        stat.setUnansweredCount(Math.toIntExact(messages.stream()
                .filter(message -> message.getNeedSupplement() != null && message.getNeedSupplement() == 1)
                .count()));
        stat.setNegativeCount(Math.toIntExact(messages.stream()
                .filter(message -> "NEGATIVE".equals(message.getEmotion())
                        || "COMPLAINT".equals(message.getEmotion()))
                .count()));
        stat.setErrorCount(Math.toIntExact(messages.stream()
                .filter(message -> message.getSuccess() != null && message.getSuccess() == 0)
                .count()));
    }

    private List<ChatMessage> loadMessages(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .ge(ChatMessage::getCreateTime, start)
                .lt(ChatMessage::getCreateTime, end));
    }

    private int countInput(List<ChatMessage> messages, String inputType) {
        return Math.toIntExact(messages.stream()
                .filter(message -> inputType.equals(message.getInputType()))
                .count());
    }

    private int avgCost(List<ChatMessage> messages) {
        return (int) Math.round(messages.stream()
                .filter(message -> message.getCostMs() != null)
                .mapToInt(ChatMessage::getCostMs)
                .average()
                .orElse(0));
    }

    private BigDecimal kbHitRate(List<ChatMessage> messages) {
        if (messages.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        long hit = messages.stream()
                .filter(message -> message.getHitKb() != null && message.getHitKb() == 1)
                .count();
        return BigDecimal.valueOf(hit * 100.0 / messages.size())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private StatDaily getStat(LocalDate date) {
        return statDailyMapper.selectOne(new LambdaQueryWrapper<StatDaily>()
                .eq(StatDaily::getStatDate, date)
                .last("LIMIT 1"));
    }

    private TodayStatVO toTodayStat(StatDaily stat) {
        TodayStatVO vo = new TodayStatVO();
        vo.setServiceCount(value(stat.getServiceCount()));
        vo.setQaCount(value(stat.getQaCount()));
        vo.setVoiceCount(value(stat.getVoiceCount()));
        vo.setImageCount(value(stat.getImageCount()));
        vo.setAvatarCount(value(stat.getAvatarCount()));
        vo.setAvgCostMs(value(stat.getAvgCostMs()));
        vo.setKbHitRate(stat.getKbHitRate() == null ? BigDecimal.ZERO.setScale(2) : stat.getKbHitRate());
        vo.setErrorCount(value(stat.getErrorCount()));
        return vo;
    }

    private List<Map<String, Object>> topQuestions(List<ChatMessage> messages) {
        return messages.stream()
                .map(ChatMessage::getQuestion)
                .filter(StringUtils::hasText)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(entry -> mapOf("question", entry.getKey(), "count", entry.getValue()))
                .toList();
    }

    private List<Map<String, Object>> topSpots(List<ChatMessage> messages) {
        return messages.stream()
                .map(ChatMessage::getSources)
                .filter(StringUtils::hasText)
                .flatMap(value -> extractSpotNames(value).stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(entry -> mapOf("spotName", entry.getKey(), "count", entry.getValue()))
                .toList();
    }

    private List<String> extractSpotNames(String sources) {
        List<String> names = new java.util.ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(sources);
            if (root.isArray()) {
                for (JsonNode item : root) {
                    String spotName = item.path("spotName").asText(null);
                    if (StringUtils.hasText(spotName)) {
                        names.add(spotName);
                    }
                }
            }
        } catch (Exception e) {
            return names;
        }
        return names;
    }

    private Map<String, Integer> emotionDist(List<ChatMessage> messages) {
        Map<String, Integer> dist = new LinkedHashMap<>();
        dist.put("positive", countEmotion(messages, "POSITIVE"));
        dist.put("neutral", countEmotion(messages, "NEUTRAL"));
        dist.put("negative", countEmotion(messages, "NEGATIVE"));
        dist.put("complaint", countEmotion(messages, "COMPLAINT"));
        return dist;
    }

    private int countEmotion(List<ChatMessage> messages, String emotion) {
        return Math.toIntExact(messages.stream().filter(message -> emotion.equals(message.getEmotion())).count());
    }

    private List<TrendVO> trend7d(LocalDate today) {
        LocalDate start = today.minusDays(6);
        List<StatDaily> stats = statDailyMapper.selectList(new LambdaQueryWrapper<StatDaily>()
                .ge(StatDaily::getStatDate, start)
                .le(StatDaily::getStatDate, today)
                .orderByAsc(StatDaily::getStatDate));
        Map<LocalDate, StatDaily> statMap = stats.stream()
                .collect(Collectors.toMap(StatDaily::getStatDate, Function.identity()));
        return java.util.stream.IntStream.rangeClosed(0, 6)
                .mapToObj(start::plusDays)
                .map(date -> toTrend(date, statMap.get(date)))
                .toList();
    }

    private TrendVO toTrend(LocalDate date, StatDaily stat) {
        if (stat == null) {
            return new TrendVO(date.toString(), 0, BigDecimal.ZERO.setScale(2));
        }
        return new TrendVO(date.toString(), value(stat.getServiceCount()),
                stat.getKbHitRate() == null ? BigDecimal.ZERO.setScale(2) : stat.getKbHitRate());
    }

    private boolean isDemoEnabled() {
        DemoSwitch demo = demoSwitchMapper.selectOne(new LambdaQueryWrapper<DemoSwitch>()
                .eq(DemoSwitch::getSwitchKey, DASHBOARD_DEMO)
                .last("LIMIT 1"));
        return demo != null && demo.getEnabled() != null && demo.getEnabled() == 1;
    }

    private DashboardOverviewVO demoOverview() {
        DashboardOverviewVO vo = new DashboardOverviewVO();
        TodayStatVO today = new TodayStatVO();
        today.setServiceCount(320);
        today.setQaCount(890);
        today.setVoiceCount(210);
        today.setImageCount(76);
        today.setAvatarCount(640);
        today.setAvgCostMs(3400);
        today.setKbHitRate(BigDecimal.valueOf(92.50));
        today.setErrorCount(3);
        vo.setToday(today);
        vo.setHotQuestions(List.of(mapOf("question", "灵山大佛多高", "count", 45)));
        vo.setHotSpots(List.of(mapOf("spotName", "灵山大佛", "count", 120)));
        vo.setEmotionDist(Map.of("positive", 210, "neutral", 480, "negative", 30, "complaint", 5));
        vo.setTrend7d(demoTrend7d());
        vo.setDemo(1);
        return vo;
    }

    private List<TrendVO> demoTrend7d() {
        LocalDate start = LocalDate.now().minusDays(6);
        return java.util.stream.IntStream.rangeClosed(0, 6)
                .mapToObj(index -> new TrendVO(start.plusDays(index).toString(),
                        260 + index * 10,
                        BigDecimal.valueOf(89.50D + index * 0.5D).setScale(2, RoundingMode.HALF_UP)))
                .toList();
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }
}
