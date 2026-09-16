package com.guido.scenicai.agent.core;

import com.guido.scenicai.agent.api.TravelAgentRequest;
import com.guido.scenicai.agent.api.TravelAgentResponse;
import com.guido.scenicai.agent.context.TravelContextBuilder;
import com.guido.scenicai.agent.harness.HarnessEngine;
import com.guido.scenicai.agent.harness.ToolRegistry;
import com.guido.scenicai.agent.harness.ToolResult;
import com.guido.scenicai.agent.intent.IntentExtractor;
import com.guido.scenicai.agent.planner.TaskPlanner;
import com.guido.scenicai.agent.planner.TravelTask;
import com.guido.scenicai.integration.amap.AmapPoiProvider;
import com.guido.scenicai.integration.amap.AmapRouteProvider;
import com.guido.scenicai.tool.map.AMapRouteTool;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TravelAgentTest {
    private final TravelAgent agent = new TravelAgent(
            new TravelContextBuilder(), new IntentExtractor(), new TaskPlanner(),
            new HarnessEngine(new ToolRegistry(java.util.List.of())));

    @Test
    void extractsFamilyTripWithMobilityConstraint() {
        TravelAgentResponse result = plan("明天带父母去杭州玩一天，父亲腿脚不太方便，预算1000，想去西湖和灵隐寺。");

        assertEquals("杭州", result.intent().city());
        assertEquals("明天", result.intent().date());
        assertEquals(1, result.intent().durationDays());
        assertEquals(new BigDecimal("1000"), result.intent().budget());
        assertEquals("父母", result.intent().travelers());
        assertEquals("FAMILY", result.intent().partyType());
        assertTrue(result.intent().mobilityConstraint());
        assertEquals(java.util.List.of("西湖", "灵隐寺"), result.intent().requestedPois());
        assertTrue(result.intent().constraints().contains("行动不便"));
        assertTask(result, TravelTask.Type.CHECK_WEATHER);
        assertTask(result, TravelTask.Type.PLAN_ROUTE);
        assertTask(result, TravelTask.Type.CHECK_BUDGET);
        assertTask(result, TravelTask.Type.VALIDATE_PLAN);
    }

    @Test
    void extractsSoloHistoryTripWithRelaxedPace() {
        TravelAgentResponse result = plan("周末一个人去西安玩两天，预算1500，喜欢历史文化，不想太赶。");

        assertEquals("西安", result.intent().city());
        assertEquals("周末", result.intent().date());
        assertEquals(2, result.intent().durationDays());
        assertEquals(new BigDecimal("1500"), result.intent().budget());
        assertEquals("SOLO", result.intent().partyType());
        assertTrue(result.intent().preferences().contains("历史文化"));
        assertTrue(result.intent().constraints().contains("低强度行程"));
        assertTask(result, TravelTask.Type.SEARCH_POI);
        assertTask(result, TravelTask.Type.PLAN_ROUTE);
        assertTask(result, TravelTask.Type.CHECK_BUDGET);
        assertEquals("EXECUTED_WITH_GAPS", result.plan().status());
    }

    @Test
    void extractsDirectWalkingRouteIntent() {
        TravelAgentResponse result = plan("帮我规划从西湖到灵隐寺的步行路线。");

        assertEquals("杭州", result.intent().city());
        assertEquals("西湖", result.intent().routeOrigin());
        assertEquals("灵隐寺", result.intent().routeDestination());
        assertEquals("walking", result.intent().routeMode());
        assertTask(result, TravelTask.Type.PLAN_ROUTE);
    }

    @Test
    void executesAgentHarnessToolProviderChain() {
        AmapPoiProvider poiProvider = mock(AmapPoiProvider.class);
        AmapRouteProvider routeProvider = mock(AmapRouteProvider.class);
        AmapPoiProvider.Coordinate westLake = new AmapPoiProvider.Coordinate(120.149, 30.259);
        AmapPoiProvider.Coordinate lingyin = new AmapPoiProvider.Coordinate(120.102, 30.240);
        when(poiProvider.search("西湖", "杭州")).thenReturn(java.util.List.of(
                new AmapPoiProvider.Poi("1", "西湖", "杭州", westLake)));
        when(poiProvider.search("灵隐寺", "杭州")).thenReturn(java.util.List.of(
                new AmapPoiProvider.Poi("2", "灵隐寺", "杭州", lingyin)));
        when(routeProvider.walking(java.util.List.of(westLake, lingyin))).thenReturn(
                new AmapRouteProvider.Route(6200, 4800, java.util.List.of(westLake, lingyin)));
        TravelAgent routeAgent = new TravelAgent(new TravelContextBuilder(), new IntentExtractor(),
                new TaskPlanner(), new HarnessEngine(new ToolRegistry(java.util.List.of(
                new AMapRouteTool(poiProvider, routeProvider)))));
        TravelAgentRequest request = new TravelAgentRequest();
        request.setMessage("帮我规划从西湖到灵隐寺的步行路线。");

        TravelAgentResponse result = routeAgent.plan(request);

        ToolResult routeResult = result.executionResults().stream()
                .filter(item -> "amap.route".equals(item.toolName())).findFirst().orElseThrow();
        assertEquals(ToolResult.Status.SUCCESS, routeResult.status());
        verify(routeProvider).walking(java.util.List.of(westLake, lingyin));
    }

    private TravelAgentResponse plan(String message) {
        TravelAgentRequest request = new TravelAgentRequest();
        request.setMessage(message);
        return agent.plan(request);
    }

    private void assertTask(TravelAgentResponse result, TravelTask.Type type) {
        assertTrue(result.tasks().stream().anyMatch(task -> task.type() == type), "missing task " + type);
    }
}
