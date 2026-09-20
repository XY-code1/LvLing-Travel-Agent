package com.guido.scenicai.agent.harness;

import com.guido.scenicai.module.city.vo.CityContextVO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** State belonging to one HarnessEngine.execute invocation only. */
public class ExecutionContext {
    private final String executionId = UUID.randomUUID().toString();
    private final Map<String, ToolResult> toolResults = new HashMap<>();
    private final Map<String, Object> attributes = new HashMap<>();
    private String cityName;
    private String cityCode;
    private CityContextVO cityContext;

    public String getExecutionId() { return executionId; }
    public String getCityName() { return cityName; }
    public String getCityCode() { return cityCode; }
    public CityContextVO getCityContext() { return cityContext; }
    public Map<String, Object> getAttributes() { return attributes; }

    public CityContextVO cityContextFor(String requestedCity) {
        return cityContext != null && cityContext.getCity() != null
                && requestedCity != null && requestedCity.trim().equals(cityName) ? cityContext : null;
    }

    public void setCityContext(CityContextVO value) {
        cityContext = value;
        cityName = value == null || value.getCity() == null ? null : value.getCity().getCityName();
        cityCode = value == null || value.getCity() == null ? null : value.getCity().getCityCode();
    }

    public ToolResult cached(String key) { return toolResults.get(key); }
    public void remember(String key, ToolResult result) { toolResults.put(key, result); }

    public static String normalized(Object value) {
        if (value == null) return "";
        if (value instanceof String text) return text.trim().toLowerCase(java.util.Locale.ROOT);
        if (value instanceof List<?> items) return items.stream().map(ExecutionContext::normalized)
                .collect(java.util.stream.Collectors.joining(","));
        return String.valueOf(value);
    }
}
