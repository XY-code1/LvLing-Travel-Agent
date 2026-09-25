package com.guido.scenicai.agent.intent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guido.scenicai.agent.api.TravelAgentRequest;
import com.guido.scenicai.agent.context.TravelContext;
import com.guido.scenicai.integration.llm.LlmChatRequest;
import com.guido.scenicai.integration.llm.LlmClientRouter;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final Pattern ROUTE = Pattern.compile("从(.+?)(?:去|到)(.+?)(?=，|,|。|!|！|怎么走|$)");
    private static final Pattern CURRENT_ROUTE = Pattern.compile("从(我?的?当前位置)去(.+?)(?:怎么走|[。.!！]|$)");
    private static final Pattern WEATHER_CITY = Pattern.compile("([\\p{IsHan}]{2,8})(?:今天|明天|后天).*天气");

    private final LlmClientRouter llmClientRouter;
    private final ObjectMapper objectMapper;

    public IntentExtractor() {
        this(null, null);
    }

    @Autowired
    public IntentExtractor(LlmClientRouter llmClientRouter, ObjectMapper objectMapper) {
        this.llmClientRouter = llmClientRouter;
        this.objectMapper = objectMapper;
    }

    /**
     * 意图抽取入口：优先走 LLM 结构化抽取，规则（正则）结果作为兜底与字段补全。
     * LLM 不可用（未注入或调用失败）时完全退化为规则抽取，行为与接入前一致。
     */
    public TravelIntent extract(TravelAgentRequest request, TravelContext context) {
        TravelIntent rules = extractByRules(request, context);
        if (llmClientRouter == null) {
            return rules;
        }
        TravelIntent llm = extractByLlm(request, context);
        return llm == null ? rules : merge(rules, llm);
    }

    private TravelIntent extractByRules(TravelAgentRequest request, TravelContext context) {
        String message = request.getMessage().trim();
        String travelers = first(context.travelers(), detectTravelers(message));
        Set<String> constraints = new LinkedHashSet<>(context.constraints());
        boolean mobilityConstraint = containsAny(message, "腿脚不太方便", "腿脚不便", "行动不便", "少走路", "走路不方便", "走路不太方便");
        if (mobilityConstraint) constraints.add("行动不便");
        if (containsAny(message, "不想太赶", "不要太赶", "不想太累", "轻松一点")) constraints.add("低强度行程");

        Set<String> preferences = new LinkedHashSet<>(context.preferences());
        addIfPresent(preferences, message, "历史文化", "历史", "文化");
        addIfPresent(preferences, message, "美食", "美食");
        addIfPresent(preferences, message, "自然风光", "自然", "风景");
        addIfPresent(preferences, message, "亲子", "亲子");
        addIfPresent(preferences, message, "摄影", "拍照", "摄影");

        RouteIntent route = routeIntent(message);
        List<String> requestedPois = requestedPois(message);
        if (requestedPois.isEmpty() && route != null) {
            requestedPois = List.of(route.origin(), route.destination());
        }
        return new TravelIntent(
                first(detectCity(message), context.city()),
                first(context.date(), detectDate(message)),
                first(context.durationDays(), parseDays(message)),
                first(context.budget(), parseBudget(message)),
                travelers,
                detectPartyType(message, travelers),
                mobilityConstraint,
                List.copyOf(preferences),
                List.copyOf(constraints),
                requestedPois,
                route == null ? null : route.origin(),
                route == null ? null : route.destination(),
                route == null ? null : route.mode());
    }

    private TravelIntent extractByLlm(TravelAgentRequest request, TravelContext context) {
        try {
            LlmChatRequest llmRequest = new LlmChatRequest();
            llmRequest.setSystemPrompt("你是旅行需求解析器。把用户消息解析成一个 JSON 对象，只输出 JSON，"
                    + "不要输出 Markdown 代码块、解释或任何其他文字。字段：city(目标城市)、date(出发日期，优先用\"今天\"/\"明天\"/\"后天\"/\"周末\"，否则用具体日期)、"
                    + "durationDays(天数整数)、budget(预算金额数字)、travelers(同行人描述，如\"父母\"或\"2人\")、"
                    + "partyType(取 SOLO/GROUP/FAMILY/UNKNOWN)、mobilityConstraint(是否行动不便或需要少走路，布尔)、"
                    + "preferences(偏好字符串数组，如历史文化/美食/自然风光/亲子/拍照)、constraints(约束字符串数组)、"
                    + "requestedPois(想去的景点字符串数组)、routeOrigin(路线起点)、routeDestination(路线终点)、routeMode(walking 或 driving)。"
                    + "没有的字段填 null。");
            llmRequest.setUserMessage("用户消息：" + request.getMessage()
                    + (StringUtils.hasText(context.city()) ? "\n已知城市：" + context.city() : "")
                    + (StringUtils.hasText(context.date()) ? "\n已知日期：" + context.date() : ""));
            llmRequest.setTemperature(0.1);
            llmRequest.setMaxTokens(400);
            String content = llmClientRouter.chat(llmRequest).getContent();
            return parseLlm(content);
        } catch (Exception ignored) {
            return null;
        }
    }

    private TravelIntent parseLlm(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String json = extractJson(raw);
        if (json == null) {
            return null;
        }
        try {
            ObjectMapper mapper = objectMapper != null ? objectMapper : new ObjectMapper();
            LlmIntent parsed = mapper.readValue(json, LlmIntent.class);
            return toIntent(parsed);
        } catch (Exception ignored) {
            return null;
        }
    }

    private TravelIntent toIntent(LlmIntent parsed) {
        if (parsed == null) {
            return null;
        }
        boolean hasSignal = StringUtils.hasText(parsed.city)
                || StringUtils.hasText(parsed.date)
                || parsed.durationDays != null
                || parsed.budget != null
                || StringUtils.hasText(parsed.travelers)
                || StringUtils.hasText(parsed.partyType)
                || parsed.mobilityConstraint != null
                || !cleanList(parsed.preferences).isEmpty()
                || !cleanList(parsed.constraints).isEmpty()
                || !cleanList(parsed.requestedPois).isEmpty()
                || StringUtils.hasText(parsed.routeOrigin)
                || StringUtils.hasText(parsed.routeDestination)
                || StringUtils.hasText(parsed.routeMode);
        if (!hasSignal) {
            return null;
        }
        return new TravelIntent(
                parsed.city,
                parsed.date,
                parsed.durationDays,
                parsed.budget,
                parsed.travelers,
                parsed.partyType,
                Boolean.TRUE.equals(parsed.mobilityConstraint),
                cleanList(parsed.preferences),
                cleanList(parsed.constraints),
                cleanList(parsed.requestedPois),
                parsed.routeOrigin,
                parsed.routeDestination,
                parsed.routeMode);
    }

    private TravelIntent merge(TravelIntent rules, TravelIntent llm) {
        return new TravelIntent(
                pick(llm.city(), rules.city()),
                pickDate(llm.date(), rules.date()),
                llm.durationDays() != null ? llm.durationDays() : rules.durationDays(),
                llm.budget() != null ? llm.budget() : rules.budget(),
                pick(llm.travelers(), rules.travelers()),
                pick(llm.partyType(), rules.partyType()),
                llm.mobilityConstraint() || rules.mobilityConstraint(),
                mergeLists(llm.preferences(), rules.preferences()),
                mergeLists(llm.constraints(), rules.constraints()),
                mergeLists(llm.requestedPois(), rules.requestedPois()),
                pick(llm.routeOrigin(), rules.routeOrigin()),
                pick(llm.routeDestination(), rules.routeDestination()),
                pick(llm.routeMode(), rules.routeMode()));
    }

    private String pick(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private String pickDate(String primary, String fallback) {
        if (!StringUtils.hasText(primary)) {
            return fallback;
        }
        // 天气工具依赖"今天/明天/后天"相对日期；LLM 若返回绝对日期，则优先保留规则得到的相对日期。
        if (StringUtils.hasText(fallback) && !isRelativeDate(primary)) {
            return fallback;
        }
        return primary;
    }

    private boolean isRelativeDate(String value) {
        return "今天".equals(value) || "明天".equals(value) || "后天".equals(value) || "周末".equals(value);
    }

    private List<String> mergeLists(List<String> primary, List<String> fallback) {
        Set<String> merged = new LinkedHashSet<>();
        if (primary != null) merged.addAll(primary);
        if (fallback != null) merged.addAll(fallback);
        return List.copyOf(merged);
    }

    private List<String> cleanList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private String extractJson(String raw) {
        String trimmed = raw.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return trimmed.substring(start, end + 1);
    }

    private String detectDate(String message) {
        if (message.contains("今天")) return "今天";
        if (message.contains("明天")) return "明天";
        if (message.contains("后天")) return "后天";
        if (message.contains("今天")) return "今天";
        if (message.contains("明天")) return "明天";
        if (message.contains("周末")) return "周末";
        if (message.contains("后天")) return "后天";
        return null;
    }

    private String detectCity(String message) {
        for (String known : List.of("杭州", "西安", "北京", "上海", "南京", "苏州", "成都", "重庆", "广州", "深圳", "青岛", "无锡")) {
            if (message.contains(known)) return known;
        }
        Matcher explicit = Pattern.compile("([\\p{IsHan}]{2,8})(?=(?:\\u4e24|\\u4e8c|\\u4e00|\\u4e09|\\u56db|\\u4e94|\\u516d|\\u4e03|\\u516b|\\u4e5d|\\u5341)\\u5929|\\u65c5\\u884c|\\u65c5\\u6e38)").matcher(message);
        if (explicit.find()) return explicit.group(1);
        String weatherCity = match(WEATHER_CITY, message);
        if (weatherCity != null) return weatherCity;
        String city = match(CITY, message);
        if (city != null) return city;
        if (containsAny(message, "西湖", "灵隐寺", "河坊街")) return "杭州";
        return null;
    }

    private Integer parseDays(String message) {
        Matcher modern = Pattern.compile("(\\d{1,2}|[\\p{IsHan}]{1,3})\\s*(?:\\u5929|\\u65e5)").matcher(message);
        if (modern.find()) {
            String value = modern.group(1);
            if (value.chars().allMatch(Character::isDigit)) return Integer.valueOf(value);
            Integer parsed = modernChineseNumber(value);
            if (parsed != null) return parsed;
        }
        String value = match(DAYS, message);
        if (value == null) return null;
        if (value.chars().allMatch(Character::isDigit)) return Integer.valueOf(value);
        return chineseNumber(value);
    }

    private Integer modernChineseNumber(String value) {
        if (value.equals("一")) return 1; if (value.equals("两") || value.equals("二")) return 2;
        if (value.equals("三")) return 3; if (value.equals("四")) return 4; if (value.equals("五")) return 5;
        if (value.equals("六")) return 6; if (value.equals("七")) return 7; if (value.equals("八")) return 8;
        if (value.equals("九")) return 9; if (value.equals("十")) return 10;
        return null;
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
        Matcher modern = Pattern.compile("\\u9884\\u7b97\\s*(?:\\u4e3a|\\u662f)?\\s*[￥¥]?\\s*([0-9,]+)").matcher(message);
        if (modern.find()) return new BigDecimal(modern.group(1).replace(",", ""));
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
        List<String> known = List.of("西湖", "灵隐寺", "兵马俑", "古城墙", "大雁塔", "钟楼", "鼓楼").stream()
                .filter(message::contains).toList();
        if (!known.isEmpty()) return known;
        String segment = match(POIS, message);
        if (segment == null) return List.of();
        List<String> result = new ArrayList<>();
        for (String item : segment.split("和|、|以及|还有|,")) {
            String value = item.trim().replaceAll("^(?:去|游览)", "");
            if (StringUtils.hasText(value) && value.length() <= 30) result.add(value);
        }
        return result.stream().distinct().toList();
    }

    private RouteIntent routeIntent(String message) {
        Matcher current = CURRENT_ROUTE.matcher(message);
        if (current.find()) return new RouteIntent(current.group(1).trim(), current.group(2).trim(), "walking");
        Matcher matcher = ROUTE.matcher(message);
        if (!matcher.find()) return null;
        String mode = containsAny(message, "驾车", "开车") ? "driving" : "walking";
        String destination = matcher.group(2).trim().replaceFirst("(?:的)?(?:步行|驾车|开车)?路线$", "");
        return new RouteIntent(matcher.group(1).trim(), destination, mode);
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

    private record RouteIntent(String origin, String destination, String mode) {
    }

    /** LLM 结构化抽取结果的载体；Jackson 按字段名映射，缺失字段为 null。 */
    public static class LlmIntent {
        public String city;
        public String date;
        public Integer durationDays;
        public BigDecimal budget;
        public String travelers;
        public String partyType;
        public Boolean mobilityConstraint;
        public List<String> preferences;
        public List<String> constraints;
        public List<String> requestedPois;
        public String routeOrigin;
        public String routeDestination;
        public String routeMode;
    }
}
