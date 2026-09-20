package com.guido.scenicai.agent.skill;

import com.guido.scenicai.agent.harness.ExecutionTrace;
import com.guido.scenicai.agent.harness.ValidationResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record TravelPlanningResult(
        String summary,
        String task,
        TravelPlanningRequest request,
        TravelIntent intent,
        Object cityContext,
        Object weather,
        Object pois,
        List<ItineraryItem> itinerary,
        Object routes,
        Object services,
        Object budget,
        Object knowledge,
        BigDecimal estimatedBudget,
        List<String> warnings,
        ValidationResult validation,
        List<ExecutionTrace> toolTrace) {
    public TravelPlanningResult {
        itinerary = itinerary == null ? List.of() : List.copyOf(itinerary);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        toolTrace = toolTrace == null ? List.of() : List.copyOf(toolTrace);
    }

    public record ItineraryItem(String time, String poi, String activity, Integer durationMinutes,
                                String transport, Integer distanceMeters, BigDecimal estimatedCost,
                                String reason) {}
}
