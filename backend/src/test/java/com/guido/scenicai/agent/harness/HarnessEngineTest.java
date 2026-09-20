package com.guido.scenicai.agent.harness;

import com.guido.scenicai.agent.context.TravelContext;
import com.guido.scenicai.agent.intent.TravelIntent;
import com.guido.scenicai.agent.planner.TravelTask;
import com.guido.scenicai.domain.trip.TravelPlan;
import com.guido.scenicai.domain.route.RouteResult;
import com.guido.scenicai.integration.amap.AmapPoiProvider;
import com.guido.scenicai.integration.amap.AmapRouteProvider;
import com.guido.scenicai.tool.map.AMapRouteTool;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HarnessEngineTest {
    @Test
    void registryFindsAmapRouteToolForPlanRoute() {
        AMapRouteTool tool = new AMapRouteTool(mock(AmapPoiProvider.class), mock(AmapRouteProvider.class));
        ToolRegistry registry = new ToolRegistry(List.of(tool));

        assertSame(tool, registry.find(TravelTask.Type.PLAN_ROUTE).orElseThrow());
    }

    @Test
    void skipsTaskWhenToolIsNotAvailable() {
        HarnessEngine engine = new HarnessEngine(new ToolRegistry(List.of()));
        TravelTask task = new TravelTask(1, TravelTask.Type.CHECK_WEATHER, "检查天气", false);

        HarnessResult result = engine.execute(new TravelPlan("DRAFT", "test", List.of(task)),
                new HarnessContext(context(), intent(null, null, null)));

        assertEquals(ToolResult.Status.SKIPPED, result.executionResults().get(0).status());
        assertEquals("TOOL_NOT_AVAILABLE", result.executionResults().get(0).errorCode());
        assertEquals(ToolResult.Status.SKIPPED, result.executionTrace().get(0).status());
    }

    @Test
    void amapRouteToolResolvesPoisAndCallsExistingProvider() {
        AmapPoiProvider poiProvider = mock(AmapPoiProvider.class);
        AmapRouteProvider routeProvider = mock(AmapRouteProvider.class);
        AmapPoiProvider.Coordinate westLake = new AmapPoiProvider.Coordinate(120.149, 30.259);
        AmapPoiProvider.Coordinate lingyin = new AmapPoiProvider.Coordinate(120.102, 30.240);
        when(poiProvider.search("西湖", "杭州")).thenReturn(List.of(
                new AmapPoiProvider.Poi("1", "西湖", "杭州", westLake)));
        when(poiProvider.search("灵隐寺", "杭州")).thenReturn(List.of(
                new AmapPoiProvider.Poi("2", "灵隐寺", "杭州", lingyin)));
        when(routeProvider.walking(List.of(westLake, lingyin))).thenReturn(
                new RouteResult(westLake, lingyin, 6200, 4800, "walking",
                        List.of(westLake, lingyin), List.of(), "amap"));
        AMapRouteTool tool = new AMapRouteTool(poiProvider, routeProvider);
        ToolRequest request = new ToolRequest(
                new TravelTask(1, TravelTask.Type.PLAN_ROUTE, "规划路线", true),
                context(), intent("西湖", "灵隐寺", "walking"));

        ToolResult result = tool.execute(request);

        assertEquals(ToolResult.Status.SUCCESS, result.status());
        AMapRouteTool.RouteOutput output = assertInstanceOf(AMapRouteTool.RouteOutput.class, result.data());
        assertEquals(6200, output.distanceMeters());
        verify(routeProvider).walking(List.of(westLake, lingyin));
    }

    @Test
    void amapRouteToolUsesCurrentLocationWithoutPoiLookup() {
        AmapPoiProvider poiProvider = mock(AmapPoiProvider.class);
        AmapRouteProvider routeProvider = mock(AmapRouteProvider.class);
        AmapPoiProvider.Coordinate current = new AmapPoiProvider.Coordinate(120.12, 30.22);
        AmapPoiProvider.Coordinate lingyin = new AmapPoiProvider.Coordinate(120.102, 30.240);
        when(poiProvider.search("灵隐寺", "杭州")).thenReturn(List.of(
                new AmapPoiProvider.Poi("2", "灵隐寺", "杭州", lingyin)));
        when(routeProvider.walking(List.of(current, lingyin))).thenReturn(
                new RouteResult(current, lingyin, 3200, 2400, "walking", List.of(current, lingyin), List.of(), "amap"));
        AMapRouteTool tool = new AMapRouteTool(poiProvider, routeProvider);
        TravelContext located = new TravelContext("杭州", null, 1, null, null,
                List.of(), List.of(), null, current.longitude(), current.latitude());

        ToolResult result = tool.execute(new ToolRequest(
                new TravelTask(1, TravelTask.Type.PLAN_ROUTE, "规划路线", true), located,
                intent("我当前位置", "灵隐寺", "walking")));

        assertEquals(ToolResult.Status.SUCCESS, result.status());
        verify(poiProvider, never()).search("我当前位置", "杭州");
        verify(routeProvider).walking(List.of(current, lingyin));
    }

    @Test
    void amapRouteToolRequiresCoordinatesForCurrentLocation() {
        AmapPoiProvider poiProvider = mock(AmapPoiProvider.class);
        AmapRouteProvider routeProvider = mock(AmapRouteProvider.class);
        AMapRouteTool tool = new AMapRouteTool(poiProvider, routeProvider);

        ToolResult result = tool.execute(new ToolRequest(
                new TravelTask(1, TravelTask.Type.PLAN_ROUTE, "规划路线", true), context(),
                intent("我的当前位置", "灵隐寺", "walking")));

        assertEquals(ToolResult.Status.FAILED, result.status());
        assertEquals("LOCATION_REQUIRED", result.errorCode());
        verifyNoInteractions(poiProvider, routeProvider);
    }

    private TravelContext context() {
        return new TravelContext("杭州", null, 1, null, null, List.of(), List.of(), null, null, null);
    }

    private TravelIntent intent(String origin, String destination, String mode) {
        List<String> pois = origin == null ? List.of() : List.of(origin, destination);
        return new TravelIntent("杭州", null, 1, null, null, "UNKNOWN", false,
                List.of(), List.of(), pois, origin, destination, mode);
    }
}
