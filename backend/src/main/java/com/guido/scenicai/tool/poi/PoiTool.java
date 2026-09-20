package com.guido.scenicai.tool.poi;

import com.guido.scenicai.agent.harness.AgentTool;
import com.guido.scenicai.agent.harness.ToolRequest;
import com.guido.scenicai.agent.harness.ToolResult;
import com.guido.scenicai.agent.planner.TravelTask;
import com.guido.scenicai.integration.amap.AmapPoiProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PoiTool implements AgentTool {
    private final AmapPoiProvider poiProvider;

    @Override
    public String toolName() { return "amap.poi"; }

    @Override
    public TravelTask.Type supportedTaskType() { return TravelTask.Type.SEARCH_POI; }

    @Override
    public ToolResult execute(ToolRequest request) {
        String city = request.context().city();
        if (city == null || city.isBlank()) {
            return ToolResult.failed(request, toolName(), "POI_CITY_REQUIRED", "POI 查询需要城市");
        }
        List<String> queries = request.intent().requestedPois().isEmpty()
                ? List.of("旅游景点") : request.intent().requestedPois();
        List<PoiSelection> result = queries.stream().map(query ->
                new PoiSelection(query, poiProvider.search(query, city))).toList();
        return ToolResult.success(request, toolName(), result);
    }

    public record PoiSelection(String query, List<AmapPoiProvider.Poi> candidates) {}
}
