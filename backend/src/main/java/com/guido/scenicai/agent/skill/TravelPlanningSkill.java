package com.guido.scenicai.agent.skill;

import com.guido.scenicai.agent.api.TravelAgentRequest;
import com.guido.scenicai.agent.context.TravelContext;
import com.guido.scenicai.agent.context.TravelContextBuilder;
import com.guido.scenicai.agent.harness.HarnessContext;
import com.guido.scenicai.agent.harness.HarnessEngine;
import com.guido.scenicai.agent.harness.HarnessResult;
import com.guido.scenicai.agent.harness.ToolResult;
import com.guido.scenicai.agent.harness.ValidationResult;
import com.guido.scenicai.agent.intent.IntentExtractor;
import com.guido.scenicai.agent.planner.TaskPlanner;
import com.guido.scenicai.agent.planner.TravelTask;
import com.guido.scenicai.domain.trip.TravelPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.time.Instant;
import java.math.BigDecimal;
import com.guido.scenicai.tool.budget.BudgetTool;
import com.guido.scenicai.tool.map.AMapRouteTool;

@Service
@RequiredArgsConstructor
public class TravelPlanningSkill {
    private final TravelContextBuilder contextBuilder;
    private final IntentExtractor intentExtractor;
    private final TaskPlanner taskPlanner;
    private final HarnessEngine harnessEngine;

    public TravelPlanningResult plan(TravelPlanningRequest request) {
        TravelAgentRequest apiRequest = toAgentRequest(request);
        TravelContext initial = contextBuilder.build(apiRequest);
        com.guido.scenicai.agent.intent.TravelIntent parsed = intentExtractor.extract(apiRequest, initial);
        TravelContext context = contextBuilder.enrich(initial, parsed);
        List<TravelTask> tasks = taskPlanner.plan(parsed, context);
        String city = context.city() == null ? "待确认城市" : context.city();
        HarnessResult executed = harnessEngine.execute(
                new TravelPlan("DRAFT", city + "旅行规划", tasks),
                new HarnessContext(context, parsed));
        ValidationResult validation = validation(executed);
        List<String> warnings = executed.executionResults().stream()
                .filter(item -> item.status() != ToolResult.Status.SUCCESS)
                .map(item -> item.message() == null ? item.errorCode() : item.message())
                .toList();
        List<TravelPlanningResult.ItineraryItem> itinerary = itinerary(executed, parsed);
        List<com.guido.scenicai.agent.harness.ExecutionTrace> trace = new ArrayList<>();
        trace.add(syntheticTrace(0, "UNDERSTAND_REQUIREMENT", "已解析旅行需求"));
        trace.add(syntheticTrace(0, "RESOLVE_DESTINATION", "已识别目的地：" + city));
        trace.addAll(executed.executionTrace());
        trace.add(syntheticTrace(tasks.size() + 1, "GENERATE_RESULT", "已生成结构化旅行规划"));
        Object budget = data(executed, "budget.rule");
        BigDecimal estimatedBudget = budget instanceof BudgetTool.BudgetEstimate value ? value.knownEstimate() : null;
        return new TravelPlanningResult(city + "旅行规划已完成工具编排", apiRequest.getMessage(), request,
                TravelIntent.from(parsed), data(executed, "city.context"), data(executed, "amap.weather"),
                data(executed, "amap.poi"), itinerary, data(executed, "amap.route"),
                data(executed, "city.services"), budget, data(executed, "rag.local"), estimatedBudget,
                warnings, validation, trace);
    }

    private Object data(HarnessResult result, String toolName) {
        return result.executionResults().stream().filter(item -> toolName.equals(item.toolName()))
                .findFirst().map(ToolResult::data).orElse(null);
    }

    private ValidationResult validation(HarnessResult result) {
        Object value = data(result, "plan.validator");
        return value instanceof ValidationResult validation ? validation : ValidationResult.from(result);
    }

    private List<TravelPlanningResult.ItineraryItem> itinerary(HarnessResult result,
            com.guido.scenicai.agent.intent.TravelIntent intent) {
        Object value = data(result, "amap.route");
        if (!(value instanceof AMapRouteTool.RouteOutput route)) return List.of();
        int travelMinutes = Math.max(1, route.durationSeconds() / 60);
        String transport = intent.mobilityConstraint() && route.distanceMeters() > 3000
                ? "建议接驳/驾车（当前真实路线为步行）" : "步行";
        return List.of(
                new TravelPlanningResult.ItineraryItem("09:00", route.origin(), "游览必去景点", 120,
                        null, null, null, "来自用户 mustVisit 与高德 POI"),
                new TravelPlanningResult.ItineraryItem("11:00", route.destination(), "前往下一景点",
                        travelMinutes, transport, route.distanceMeters(), null,
                        "距离和时间来自高德真实路线；低行动能力时提示调整交通方式"));
    }

    private String statusFor(TravelTask task, HarnessResult result) {
        return result.executionResults().stream().filter(item -> item.taskId() == task.order())
                .findFirst().map(item -> item.status().name()).orElse("PENDING");
    }

    private TravelAgentRequest toAgentRequest(TravelPlanningRequest request) {
        TravelAgentRequest result = new TravelAgentRequest();
        result.setMessage(request.message() == null ? buildMessage(request) : request.message());
        result.setCity(request.city());
        result.setDate(request.date());
        result.setDurationDays(request.duration());
        result.setBudget(request.budget());
        result.setTravelers(request.travelers());
        result.setPreferences(request.preferences());
        result.setConstraints(request.mobility() == null ? List.of() : List.of(request.mobility()));
        result.setLongitude(request.longitude());
        result.setLatitude(request.latitude());
        return result;
    }

    private String buildMessage(TravelPlanningRequest request) {
        return (request.date() == null ? "" : request.date()) + "带" +
                (request.travelers() == null ? "同行人" : request.travelers()) + "去" +
                (request.city() == null ? "当前城市" : request.city()) + "旅行" +
                (request.duration() == null ? "" : request.duration() + "天") +
                (request.mustVisit().isEmpty() ? "" : "，想去" + String.join("和", request.mustVisit()));
    }

    private com.guido.scenicai.agent.harness.ExecutionTrace syntheticTrace(int step, String name, String message) {
        Instant now = Instant.now();
        return new com.guido.scenicai.agent.harness.ExecutionTrace(step, name, "travel-planning",
                ToolResult.Status.SUCCESS, now, now, 0, message,
                null, message, "skill", null);
    }
}
