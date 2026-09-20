package com.guido.scenicai.tool.city;

import com.guido.scenicai.agent.harness.AgentTool;
import com.guido.scenicai.agent.harness.ToolRequest;
import com.guido.scenicai.agent.harness.ToolResult;
import com.guido.scenicai.agent.planner.TravelTask;
import com.guido.scenicai.module.city.service.CityDiscoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CityContextTool implements AgentTool {
    private final CityDiscoveryService cityDiscoveryService;

    @Override
    public String toolName() { return "city.context"; }

    @Override
    public TravelTask.Type supportedTaskType() { return TravelTask.Type.RESOLVE_CITY; }

    @Override
    public ToolResult execute(ToolRequest request) {
        String city = request.context().city();
        if (city == null || city.isBlank()) {
            return ToolResult.failed(request, toolName(), "CITY_REQUIRED", "城市上下文解析需要城市");
        }
        var existing = request.executionContext().cityContextFor(city);
        if (existing != null) {
            request.executionContext().getAttributes().put("cityContextEvent", "CITY_CONTEXT_REUSED");
            return ToolResult.success(request, toolName(), existing);
        }
        var resolved = cityDiscoveryService.discover(null, city);
        request.executionContext().setCityContext(resolved);
        request.executionContext().getAttributes().put("cityContextEvent", "CITY_CONTEXT_RESOLVED");
        return ToolResult.success(request, toolName(), resolved);
    }
}
