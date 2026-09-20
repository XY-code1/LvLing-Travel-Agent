package com.guido.scenicai.tool.map;

import com.guido.scenicai.agent.harness.AgentTool;
import com.guido.scenicai.agent.harness.ToolRequest;
import com.guido.scenicai.agent.harness.ToolResult;
import com.guido.scenicai.agent.intent.TravelIntent;
import com.guido.scenicai.agent.planner.TravelTask;
import com.guido.scenicai.integration.amap.AmapPoiProvider;
import com.guido.scenicai.integration.amap.AmapRouteProvider;
import com.guido.scenicai.domain.route.RouteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import com.guido.scenicai.tool.poi.PoiTool;

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
        if (origin == null || destination == null) {
            List<AmapPoiProvider.Poi> candidates = request.previousResults().stream()
                    .filter(result -> "amap.poi".equals(result.toolName()) && result.data() instanceof List<?>)
                    .flatMap(result -> ((List<?>) result.data()).stream())
                    .filter(PoiTool.PoiSelection.class::isInstance).map(PoiTool.PoiSelection.class::cast)
                    .flatMap(selection -> selection.candidates().stream()).limit(2).toList();
            if (candidates.size() >= 2) {
                origin = candidates.get(0).name();
                destination = candidates.get(1).name();
            }
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

        boolean currentLocation = origin.contains("我当前位置") || origin.contains("当前位置");
        AmapPoiProvider.Poi originPoi;
        if (currentLocation) {
            if (request.context().longitude() == null || request.context().latitude() == null) {
                return ToolResult.failed(request, toolName(), "LOCATION_REQUIRED", "需要授权定位后才能从当前位置规划路线");
            }
            originPoi = new AmapPoiProvider.Poi("current-location", "我的位置", city,
                    new AmapPoiProvider.Coordinate(request.context().longitude(), request.context().latitude()));
        } else {
            originPoi = firstPoi(request, origin, city);
        }
        AmapPoiProvider.Poi destinationPoi = firstPoi(request, destination, city);
        if (originPoi == null || destinationPoi == null) {
            return ToolResult.failed(request, toolName(), "ROUTE_POI_NOT_FOUND", "高德未找到路线起点或终点");
        }
        RouteResult route = routeProvider.walking(List.of(
                originPoi.coordinates(), destinationPoi.coordinates()));
        return ToolResult.success(request, toolName(), new RouteOutput(
                originPoi.name(), destinationPoi.name(), city, mode,
                originPoi.coordinates(), destinationPoi.coordinates(), route.distanceMeters(),
                route.durationSeconds(), route.polyline(), route.steps(), route.provider()));
    }

    private AmapPoiProvider.Poi firstPoi(ToolRequest request, String name, String city) {
        AmapPoiProvider.Poi existing = request.previousResults().stream()
                .filter(result -> "amap.poi".equals(result.toolName()) && result.data() instanceof List<?>)
                .flatMap(result -> ((List<?>) result.data()).stream())
                .filter(PoiTool.PoiSelection.class::isInstance).map(PoiTool.PoiSelection.class::cast)
                .filter(selection -> name.equals(selection.query()))
                .flatMap(selection -> selection.candidates().stream()).findFirst().orElse(null);
        if (existing != null) return existing;
        List<AmapPoiProvider.Poi> pois = poiProvider.search(name, city);
        return pois.isEmpty() ? null : pois.get(0);
    }

    public record RouteOutput(
            String origin,
            String destination,
            String city,
            String routeMode,
            AmapPoiProvider.Coordinate originCoordinates,
            AmapPoiProvider.Coordinate destinationCoordinates,
            int distanceMeters,
            int durationSeconds,
            List<AmapPoiProvider.Coordinate> polyline,
            List<RouteResult.Step> steps,
            String provider) {
    }
}
