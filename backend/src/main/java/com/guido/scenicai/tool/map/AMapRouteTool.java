package com.guido.scenicai.tool.map;

import com.guido.scenicai.agent.harness.AgentTool;
import com.guido.scenicai.agent.harness.ToolRequest;
import com.guido.scenicai.agent.harness.ToolResult;
import com.guido.scenicai.agent.intent.TravelIntent;
import com.guido.scenicai.agent.planner.TravelTask;
import com.guido.scenicai.integration.amap.AmapPoiProvider;
import com.guido.scenicai.integration.amap.AmapRouteProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AMapRouteTool implements AgentTool {
    private final AmapPoiProvider poiProvider;
    private final AmapRouteProvider routeProvider;

    @Override
    public String toolName() {
        return "amap.route";
    }

    @Override
    public TravelTask.Type supportedTaskType() {
        return TravelTask.Type.PLAN_ROUTE;
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        TravelIntent intent = request.intent();
        String origin = intent.routeOrigin();
        String destination = intent.routeDestination();
        if ((origin == null || destination == null) && intent.requestedPois().size() >= 2) {
            origin = intent.requestedPois().get(0);
            destination = intent.requestedPois().get(intent.requestedPois().size() - 1);
        }
        String city = request.context().city();
        String mode = intent.routeMode() == null ? "walking" : intent.routeMode();
        if (!"walking".equals(mode)) {
            return ToolResult.failed(request, toolName(), "ROUTE_MODE_NOT_AVAILABLE",
                    "当前 AMapRouteProvider 仅支持步行路线");
        }
        if (origin == null || destination == null || city == null) {
            return ToolResult.failed(request, toolName(), "ROUTE_INPUT_INCOMPLETE",
                    "路线执行需要起点、终点和城市");
        }

        AmapPoiProvider.Poi originPoi = firstPoi(origin, city);
        AmapPoiProvider.Poi destinationPoi = firstPoi(destination, city);
        if (originPoi == null || destinationPoi == null) {
            return ToolResult.failed(request, toolName(), "ROUTE_POI_NOT_FOUND", "高德未找到路线起点或终点");
        }
        AmapRouteProvider.Route route = routeProvider.walking(List.of(
                originPoi.coordinates(), destinationPoi.coordinates()));
        return ToolResult.success(request, toolName(), new RouteOutput(
                originPoi.name(), destinationPoi.name(), city, mode,
                route.distanceMeters(), route.durationSeconds(), route.polyline()));
    }

    private AmapPoiProvider.Poi firstPoi(String name, String city) {
        List<AmapPoiProvider.Poi> pois = poiProvider.search(name, city);
        return pois.isEmpty() ? null : pois.get(0);
    }

    public record RouteOutput(
            String origin,
            String destination,
            String city,
            String routeMode,
            int distanceMeters,
            int durationSeconds,
            List<AmapPoiProvider.Coordinate> polyline) {
    }
}
