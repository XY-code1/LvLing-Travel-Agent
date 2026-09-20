package com.guido.scenicai.tool.service;

import com.guido.scenicai.agent.harness.AgentTool;
import com.guido.scenicai.agent.harness.ToolRequest;
import com.guido.scenicai.agent.harness.ToolResult;
import com.guido.scenicai.agent.planner.TravelTask;
import com.guido.scenicai.module.city.vo.CityContextVO;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.guido.scenicai.integration.amap.AmapPoiProvider;
import java.util.List;

@Component
public class ServiceTool implements AgentTool {
    private final AmapPoiProvider poiProvider;

    public ServiceTool() { this.poiProvider = null; }

    @Autowired
    public ServiceTool(AmapPoiProvider poiProvider) { this.poiProvider = poiProvider; }
    @Override
    public String toolName() { return "city.services"; }

    @Override
    public TravelTask.Type supportedTaskType() { return TravelTask.Type.QUERY_SERVICES; }

    @Override
    public ToolResult execute(ToolRequest request) {
        CityContextVO context = request.previousResults().stream()
                .filter(result -> "city.context".equals(result.toolName()))
                .map(ToolResult::data).filter(CityContextVO.class::isInstance)
                .map(CityContextVO.class::cast).findFirst().orElse(null);
        if (context == null) {
            return ToolResult.failed(request, toolName(), "CITY_CONTEXT_REQUIRED", "服务查询需要已解析的城市上下文");
        }
        if (context.getServices() == null || context.getServices().isEmpty()) {
            String cityName = context.getCity() == null ? request.context().city() : context.getCity().getCityName();
            String keyword = request.intent().mobilityConstraint() ? "无障碍设施" : "游客服务中心";
            if (poiProvider == null) return ToolResult.degraded(request, toolName(), List.of(), "SERVICES_NOT_FOUND",
                    cityName + "暂无已验证的服务设施数据");
            List<AmapPoiProvider.Poi> services = poiProvider.search(keyword, cityName);
            if (services.isEmpty()) return ToolResult.degraded(request, toolName(), List.of(), "SERVICES_NOT_FOUND",
                    cityName + "暂无已验证的服务设施数据");
            return ToolResult.success(request, toolName(), services);
        }
        return ToolResult.success(request, toolName(), context.getServices());
    }
}
