package com.guido.scenicai.agent.api;

import com.guido.scenicai.agent.context.TravelContext;
import com.guido.scenicai.agent.harness.ExecutionTrace;
import com.guido.scenicai.agent.harness.ToolResult;
import com.guido.scenicai.agent.intent.TravelIntent;
import com.guido.scenicai.agent.planner.TravelTask;
import com.guido.scenicai.domain.trip.TravelPlan;

import java.util.List;
import java.math.BigDecimal;

public record TravelAgentResponse(
        TravelContext context,
        TravelIntent intent,
        List<TravelTask> tasks,
        TravelPlan plan,
        List<ToolResult> executionResults,
        List<ExecutionTrace> executionTrace,
        String message,
        long latencyMs,
        boolean llmCalled,
        boolean fallback,
        List<ItineraryDay> itinerary,
        BudgetSummary budget) {
    public record ItineraryDay(int day, String theme, List<ItineraryItem> items) {}
    public record ItineraryItem(String time, String name, String type, int durationMinutes,
                                String reason, Double longitude, Double latitude, int sequence) {}
    public record BudgetSummary(BigDecimal total, BigDecimal remaining) {}
}
