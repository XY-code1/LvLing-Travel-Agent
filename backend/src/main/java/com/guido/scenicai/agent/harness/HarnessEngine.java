package com.guido.scenicai.agent.harness;

import com.guido.scenicai.agent.planner.TravelTask;
import com.guido.scenicai.domain.trip.TravelPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HarnessEngine {
    private final ToolRegistry toolRegistry;

    public HarnessResult execute(TravelPlan plan, HarnessContext context) {
        List<ToolResult> results = new ArrayList<>();
        List<ExecutionTrace> trace = new ArrayList<>();
        for (TravelTask task : plan.tasks()) {
            Instant start = Instant.now();
            AgentTool tool = toolRegistry.find(task.type()).orElse(null);
            ToolResult result;
            if (tool == null) {
                result = ToolResult.skipped(task.order(),
                        "TOOL_NOT_AVAILABLE", "当前阶段没有可执行该任务的 Tool");
            } else {
                try {
                    result = tool.execute(new ToolRequest(task, context.travelContext(), context.intent()));
                } catch (Exception exception) {
                    result = new ToolResult(task.order(), tool.toolName(), ToolResult.Status.FAILED,
                            null, "TOOL_EXECUTION_FAILED", safeMessage(exception));
                }
            }
            Instant end = Instant.now();
            results.add(result);
            trace.add(new ExecutionTrace(task.order(), result.toolName(), result.status(), start, end,
                    Duration.between(start, end).toMillis()));
        }
        return new HarnessResult(List.copyOf(results), List.copyOf(trace));
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "Tool 执行失败" : message;
    }
}
