package com.guido.scenicai.agent.intent;

import com.guido.scenicai.agent.api.TravelAgentRequest;
import com.guido.scenicai.agent.context.TravelContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class IntentExtractor {
    private static final Pattern CITY = Pattern.compile("(?:去|到)([\\p{IsHan}]{2,8}?)(?:玩|旅行|旅游|待)");
    private static final Pattern DAYS = Pattern.compile("([0-9一二两三四五六七八九十]+)\\s*(?:天|日)");
    private static final Pattern BUDGET = Pattern.compile("预算\\s*(?:为|是)?\\s*[¥￥]?\\s*([0-9,]+)");
    private static final Pattern POIS = Pattern.compile("想去(.+?)(?:[。.!！]|$)");

    public TravelIntent extract(TravelAgentRequest request, TravelContext context) {
        String message = request.getMessage().trim();
        String travelers = first(context.travelers(), detectTravelers(message));
        Set<String> constraints = new LinkedHashSet<>(context.constraints());
        boolean mobilityConstraint = containsAny(message, "腿脚不太方便", "腿脚不便", "行动不便", "少走路");
        if (mobilityConstraint) constraints.add("行动不便");
        if (containsAny(message, "不想太赶", "不要太赶", "轻松一点")) constraints.add("低强度行程");

        Set<String> preferences = new LinkedHashSet<>(context.preferences());
        addIfPresent(preferences, message, "历史文化", "历史", "文化");
        addIfPresent(preferences, message, "美食", "美食");
        addIfPresent(preferences, message, "自然风光", "自然", "风景");
        addIfPresent(preferences, message, "亲子", "亲子");
        addIfPresent(preferences, message, "摄影", "拍照", "摄影");

        return new TravelIntent(
                first(context.city(), match(CITY, message)),
                first(context.date(), detectDate(message)),
                first(context.durationDays(), parseDays(message)),
                first(context.budget(), parseBudget(message)),
                travelers,
                detectPartyType(message, travelers),
                mobilityConstraint,
                List.copyOf(preferences),
                List.copyOf(constraints),
                requestedPois(message));
    }

    private String detectDate(String message) {
        if (message.contains("明天")) return "明天";
        if (message.contains("周末")) return "周末";
        if (message.contains("后天")) return "后天";
        return null;
    }

    private Integer parseDays(String message) {
        String value = match(DAYS, message);
        if (value == null) return null;
        if (value.chars().allMatch(Character::isDigit)) return Integer.valueOf(value);
        return chineseNumber(value);
    }

    private Integer chineseNumber(String value) {
        if ("十".equals(value)) return 10;
        if (value.contains("十")) {
            String[] parts = value.split("十", -1);
            int tens = parts[0].isEmpty() ? 1 : chineseDigit(parts[0]);
            int ones = parts.length < 2 || parts[1].isEmpty() ? 0 : chineseDigit(parts[1]);
            return tens > 0 && ones >= 0 ? tens * 10 + ones : null;
        }
        int number = chineseDigit(value);
        return number >= 0 ? number : null;
    }

    private int chineseDigit(String value) {
        return switch (value) {
            case "零" -> 0;
            case "一" -> 1;
            case "二", "两" -> 2;
            case "三" -> 3;
            case "四" -> 4;
            case "五" -> 5;
            case "六" -> 6;
            case "七" -> 7;
            case "八" -> 8;
            case "九" -> 9;
            default -> -1;
        };
    }

    private BigDecimal parseBudget(String message) {
        String value = match(BUDGET, message);
        return value == null ? null : new BigDecimal(value.replace(",", ""));
    }

    private String detectTravelers(String message) {
        if (message.contains("父母")) return "父母";
        if (containsAny(message, "一个人", "独自", "单人")) return "1人";
        Matcher matcher = Pattern.compile("([0-9]+)\\s*人").matcher(message);
        return matcher.find() ? matcher.group(1) + "人" : null;
    }

    private String detectPartyType(String message, String travelers) {
        if (containsAny(message, "一个人", "独自", "单人") || "1人".equals(travelers)) return "SOLO";
        if (containsAny(message, "父母", "老人", "孩子", "家人")) return "FAMILY";
        return StringUtils.hasText(travelers) ? "GROUP" : "UNKNOWN";
    }

    private List<String> requestedPois(String message) {
        String segment = match(POIS, message);
        if (segment == null) return List.of();
        List<String> result = new ArrayList<>();
        for (String item : segment.split("和|、|以及|还有|,")) {
            String value = item.trim().replaceAll("^(?:去|游览)", "");
            if (StringUtils.hasText(value) && value.length() <= 30) result.add(value);
        }
        return result.stream().distinct().toList();
    }

    private void addIfPresent(Set<String> target, String message, String value, String... keywords) {
        if (containsAny(message, keywords)) target.add(value);
    }

    private boolean containsAny(String message, String... values) {
        for (String value : values) if (message.contains(value)) return true;
        return false;
    }

    private String match(Pattern pattern, String message) {
        Matcher matcher = pattern.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }

    private <T> T first(T explicit, T inferred) {
        return explicit != null && (!(explicit instanceof String value) || !value.isBlank()) ? explicit : inferred;
    }
}
