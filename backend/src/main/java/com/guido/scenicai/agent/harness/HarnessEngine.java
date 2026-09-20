package com.guido.scenicai.agent.harness;

import com.guido.scenicai.agent.planner.TravelTask;
import com.guido.scenicai.domain.trip.TravelPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.MDC;

@Component
@RequiredArgsConstructor
public class HarnessEngine {
    private final ToolRegistry toolRegistry;

    public HarnessResult execute(TravelPlan plan, HarnessContext context) {
        List<ToolResult> results = new ArrayList<>();
        List<ExecutionTrace> trace = new ArrayList<>();
        ExecutionContext execution = new ExecutionContext();
        try (MDC.MDCCloseable scope = MDC.putCloseable("amapExecutionId", execution.getExecutionId())) {
        MDC.put("amapCallCount", "0");
        MDC.put("amapCacheHitCount", "0");
        for (TravelTask task : plan.tasks()) {
            Instant start = Instant.now();
            AgentTool tool = toolRegistry.find(task.type()).orElse(null);
            ToolResult result;
            boolean cacheHit = false;
            if (tool == null) {
                result = ToolResult.skipped(task.order(),
                        "TOOL_NOT_AVAILABLE", "当前阶段没有可执行该任务的 Tool");
            } else {
                try {
                    String cacheKey = cacheKey(tool.toolName(), task, context);
                    // CityContextTool performs its own semantic city match so its trace can distinguish reuse.
                    ToolResult cached = "city.context".equals(tool.toolName()) ? null : execution.cached(cacheKey);
                    if (cached != null) {
                        cacheHit = true;
                        result = new ToolResult(task.order(), cached.toolName(), cached.status(),
                                cached.data(), cached.errorCode(), cached.message());
                        if (task.type() == TravelTask.Type.RESOLVE_CITY) {
                            execution.getAttributes().put("cityContextEvent", "CITY_CONTEXT_REUSED");
                        }
                        org.slf4j.LoggerFactory.getLogger(HarnessEngine.class).debug(
                                "[AMAP_CACHE] executionId={} tool={} city={} cacheHit=true",
                                execution.getExecutionId(), tool.toolName(), context.travelContext().city());
                    } else {
                        result = tool.execute(new ToolRequest(task, context.travelContext(), context.intent(), results, execution));
                        if (!"city.context".equals(tool.toolName())
                                && result.status() != ToolResult.Status.FAILED && result.status() != ToolResult.Status.SKIPPED) {
                            execution.remember(cacheKey, result);
                        }
                    }
                } catch (Exception exception) {
                    result = new ToolResult(task.order(), tool.toolName(), ToolResult.Status.FAILED,
                            null, "TOOL_EXECUTION_FAILED", safeMessage(exception));
                }
            }
            Instant end = Instant.now();
            results.add(result);
            String event = cacheHit ? "TOOL_CACHE_HIT" : task.type() == TravelTask.Type.RESOLVE_CITY
                    ? (String) execution.getAttributes().get("cityContextEvent") : null;
            trace.add(new ExecutionTrace(task.order(), stepName(task.type()), result.toolName(), result.status(), start, end,
                    Duration.between(start, end).toMillis(), (event == null ? traceMessage(task, result)
                            : event + ": " + traceMessage(task, result))
                            + " [AMAP_CALL=" + MDC.get("amapCallCount")
                            + ", AMAP_CACHE_HIT=" + MDC.get("amapCacheHitCount") + "]",
                    inputSummary(context, task), outputSummary(result), source(result),
                    result.status() == ToolResult.Status.FAILED ? result.errorCode() + ": " + result.message() : null));
        }
        }
        return new HarnessResult(List.copyOf(results), List.copyOf(trace));
    }

    private String cacheKey(String toolName, TravelTask task, HarnessContext context) {
        var travel = context.travelContext();
        var intent = context.intent();
        // Include every input affecting the tool. A changed plan/constraint cannot reuse a stale result.
        return toolName + "|" + ExecutionContext.normalized(travel.city()) + "|"
                + ExecutionContext.normalized(intent.city()) + "|" + ExecutionContext.normalized(travel.date())
                + "|" + ExecutionContext.normalized(intent.requestedPois()) + "|"
                + ExecutionContext.normalized(intent.routeOrigin()) + "|"
                + ExecutionContext.normalized(intent.routeDestination()) + "|"
                + ExecutionContext.normalized(intent.routeMode()) + "|"
                + ExecutionContext.normalized(travel.longitude()) + "|"
                + ExecutionContext.normalized(travel.latitude()) + "|"
                + ExecutionContext.normalized(travel.budget()) + "|"
                + ExecutionContext.normalized(travel.durationDays()) + "|"
                + ExecutionContext.normalized(travel.travelers()) + "|"
                + ExecutionContext.normalized(travel.constraints()) + "|"
                + ExecutionContext.normalized(intent.constraints()) + "|" + task.type();
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "Tool 执行失败" : message;
    }

    private String stepName(TravelTask.Type type) {
        return switch (type) {
            case RESOLVE_CITY -> "RESOLVE_CITY";
            case CHECK_WEATHER -> "QUERY_WEATHER";
            case SEARCH_POI -> "QUERY_POI";
            case CHECK_POI_OPENING -> "CHECK_OPENING_HOURS";
            case PLAN_ROUTE -> "QUERY_ROUTE";
            case QUERY_SERVICES -> "QUERY_SERVICES";
            case CHECK_BUDGET -> "CHECK_BUDGET";
            case QUERY_RAG -> "QUERY_RAG";
            case VALIDATE_PLAN -> "VALIDATE_PLAN";
        };
    }

    private String inputSummary(HarnessContext context, TravelTask task) {
        return "city=" + context.travelContext().city() + ", task=" + task.type();
    }

    private String outputSummary(ToolResult result) {
        if (result.data() == null) return result.message();
        String value = String.valueOf(result.data());
        return value.length() > 300 ? value.substring(0, 300) + "..." : value;
    }

    private String source(ToolResult result) {
        if (result.toolName() == null) return null;
        if (result.toolName().startsWith("amap.")) return "amap";
        if (result.toolName().startsWith("rag.")) return "local-rag";
        if (result.toolName().startsWith("city.")) return "city-discovery";
        return "rule";
    }

    private String traceMessage(TravelTask task, ToolResult result) {
        if (result.message() != null && !result.message().isBlank()) return result.message();
        return result.status() == ToolResult.Status.SUCCESS ? task.description() + "完成" : task.description();
    }
}
