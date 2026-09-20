package com.guido.scenicai.tool.weather;

import com.guido.scenicai.agent.harness.AgentTool;
import com.guido.scenicai.agent.harness.ToolRequest;
import com.guido.scenicai.agent.harness.ToolResult;
import com.guido.scenicai.agent.planner.TravelTask;
import com.guido.scenicai.integration.amap.AmapWeatherProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WeatherTool implements AgentTool {
    private final AmapWeatherProvider weatherProvider;

    @Override
    public String toolName() { return "amap.weather"; }

    @Override
    public TravelTask.Type supportedTaskType() { return TravelTask.Type.CHECK_WEATHER; }

    @Override
    public ToolResult execute(ToolRequest request) {
        String city = request.context().city();
        if (city == null || city.isBlank()) {
            return ToolResult.failed(request, toolName(), "WEATHER_CITY_REQUIRED", "天气查询需要城市");
        }
        String date = request.context().date();
        AmapWeatherProvider.Weather weather = "明天".equals(date) ? weatherProvider.forecast(city, 1)
                : "后天".equals(date) ? weatherProvider.forecast(city, 2) : weatherProvider.current(city);
        return weather == null
                ? ToolResult.failed(request, toolName(), "WEATHER_NOT_FOUND", "高德未返回城市天气")
                : ToolResult.success(request, toolName(), weather);
    }
}
