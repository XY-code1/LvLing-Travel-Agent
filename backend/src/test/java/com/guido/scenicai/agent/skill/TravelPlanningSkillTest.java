package com.guido.scenicai.agent.skill;

import com.guido.scenicai.agent.context.TravelContextBuilder;
import com.guido.scenicai.agent.harness.HarnessEngine;
import com.guido.scenicai.agent.harness.ToolRegistry;
import com.guido.scenicai.agent.harness.ToolResult;
import com.guido.scenicai.agent.intent.IntentExtractor;
import com.guido.scenicai.agent.planner.TaskPlanner;
import com.guido.scenicai.domain.route.RouteResult;
import com.guido.scenicai.integration.amap.AmapPoiProvider;
import com.guido.scenicai.integration.amap.AmapRouteProvider;
import com.guido.scenicai.integration.amap.AmapWeatherProvider;
import com.guido.scenicai.tool.map.AMapRouteTool;
import com.guido.scenicai.tool.weather.WeatherTool;
import com.guido.scenicai.tool.city.CityContextTool;
import com.guido.scenicai.tool.poi.PoiTool;
import com.guido.scenicai.module.city.service.CityDiscoveryService;
import com.guido.scenicai.module.city.vo.CityContextVO;
import com.guido.scenicai.module.knowledge.service.LocalRagService;
import com.guido.scenicai.tool.budget.BudgetTool;
import com.guido.scenicai.tool.opening.OpeningHoursTool;
import com.guido.scenicai.tool.rag.RagTool;
import com.guido.scenicai.tool.service.ServiceTool;
import com.guido.scenicai.tool.validation.ValidatorTool;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TravelPlanningSkillTest {
    @Test
    void plansHangzhouFamilyTripThroughExistingHarnessAndTools() {
        AmapWeatherProvider weatherProvider = mock(AmapWeatherProvider.class);
        CityDiscoveryService cityDiscoveryService = mock(CityDiscoveryService.class);
        CityContextVO cityContext = new CityContextVO();
        cityContext.setServices(List.of());
        when(cityDiscoveryService.discover(null, "杭州")).thenReturn(cityContext);
        when(weatherProvider.forecast("杭州", 1)).thenReturn(new AmapWeatherProvider.Weather(
                "浙江省", "杭州市", "多云", "25", "东", "3", "", "2026-09-18 10:00:00"));
        AmapPoiProvider poiProvider = mock(AmapPoiProvider.class);
        AmapRouteProvider routeProvider = mock(AmapRouteProvider.class);
        AmapPoiProvider.Coordinate westLake = new AmapPoiProvider.Coordinate(120.149, 30.259);
        AmapPoiProvider.Coordinate lingyin = new AmapPoiProvider.Coordinate(120.102, 30.240);
        when(poiProvider.search("西湖", "杭州")).thenReturn(List.of(new AmapPoiProvider.Poi("1", "西湖", "杭州", westLake)));
        when(poiProvider.search("灵隐寺", "杭州")).thenReturn(List.of(new AmapPoiProvider.Poi("2", "灵隐寺", "杭州", lingyin)));
        when(routeProvider.walking(List.of(westLake, lingyin))).thenReturn(new RouteResult(
                westLake, lingyin, 6200, 4800, "walking", List.of(westLake, lingyin), List.of(), "amap"));
        LocalRagService ragService = mock(LocalRagService.class);
        when(ragService.retrieve(any(), anyString())).thenReturn(LocalRagService.RagResult.miss());
        TravelPlanningSkill skill = new TravelPlanningSkill(new TravelContextBuilder(), new IntentExtractor(),
                new TaskPlanner(), new HarnessEngine(new ToolRegistry(List.of(
                        new CityContextTool(cityDiscoveryService), new WeatherTool(weatherProvider),
                        new PoiTool(poiProvider), new OpeningHoursTool(),
                        new AMapRouteTool(poiProvider, routeProvider), new ServiceTool(), new BudgetTool(),
                        new RagTool(ragService), new ValidatorTool()))));
        TravelPlanningRequest request = new TravelPlanningRequest(null, null, null, null, null,
                List.of(), List.of(), null, List.of(),
                "明天带父母去杭州玩一天，父亲走路不方便，预算1000元，想去西湖和灵隐寺。", null, null);

        TravelPlanningResult result = skill.plan(request);

        assertEquals("杭州", result.intent().city());
        assertEquals(1, result.intent().duration());
        assertEquals("父母", result.intent().travelers());
        assertEquals(new BigDecimal("1000"), result.intent().budget());
        assertEquals("LOW", result.intent().mobility());
        assertEquals(List.of("西湖", "灵隐寺"), result.intent().mustVisit());
        assertTrue(result.toolTrace().stream().anyMatch(trace -> "QUERY_WEATHER".equals(trace.step())
                && trace.status() == ToolResult.Status.SUCCESS));
        assertTrue(result.toolTrace().stream().anyMatch(trace -> "QUERY_ROUTE".equals(trace.step())
                && trace.status() == ToolResult.Status.SUCCESS));
        assertTrue(result.toolTrace().stream().anyMatch(trace -> "RESOLVE_CITY".equals(trace.step())
                && trace.status() == ToolResult.Status.SUCCESS));
        assertTrue(result.toolTrace().stream().anyMatch(trace -> "QUERY_POI".equals(trace.step())
                && trace.status() == ToolResult.Status.SUCCESS));
        assertTrue(result.toolTrace().stream().anyMatch(trace -> "UNDERSTAND_REQUIREMENT".equals(trace.step())));
        assertTrue(result.toolTrace().stream().anyMatch(trace -> "GENERATE_RESULT".equals(trace.step())));
        assertEquals("PASS_WITH_WARNINGS", result.validation().validationStatus());
        assertNotNull(result.budget());
        assertFalse(result.itinerary().isEmpty());
        assertTrue(result.toolTrace().stream().anyMatch(trace -> "CHECK_OPENING_HOURS".equals(trace.step())
                && trace.status() == ToolResult.Status.DEGRADED));
        assertTrue(result.toolTrace().stream().anyMatch(trace -> "QUERY_SERVICES".equals(trace.step())));
        assertTrue(result.toolTrace().stream().anyMatch(trace -> "QUERY_RAG".equals(trace.step())));
        assertTrue(result.toolTrace().stream().anyMatch(trace -> "VALIDATE_PLAN".equals(trace.step())));
        verify(cityDiscoveryService).discover(null, "杭州");
        verify(weatherProvider).forecast("杭州", 1);
        verify(routeProvider).walking(List.of(westLake, lingyin));
    }
}
