package com.guido.scenicai.agent.core;

import com.guido.scenicai.agent.api.TravelAgentRequest;
import com.guido.scenicai.agent.api.TravelAgentResponse;
import com.guido.scenicai.agent.context.TravelContextBuilder;
import com.guido.scenicai.agent.intent.IntentExtractor;
import com.guido.scenicai.agent.planner.TaskPlanner;
import com.guido.scenicai.agent.planner.TravelTask;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TravelAgentTest {
    private final TravelAgent agent = new TravelAgent(
            new TravelContextBuilder(), new IntentExtractor(), new TaskPlanner());

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
        assertEquals("DRAFT", result.plan().status());
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
