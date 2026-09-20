package com.guido.scenicai.agent.harness;

import com.guido.scenicai.agent.context.TravelContext;
import com.guido.scenicai.agent.intent.TravelIntent;
import com.guido.scenicai.agent.planner.TravelTask;

import java.util.List;

public record ToolRequest(TravelTask task, TravelContext context, TravelIntent intent,
                          List<ToolResult> previousResults, ExecutionContext executionContext) {
    public ToolRequest(TravelTask task, TravelContext context, TravelIntent intent) {
        this(task, context, intent, List.of(), new ExecutionContext());
    }

    public ToolRequest(TravelTask task, TravelContext context, TravelIntent intent, List<ToolResult> previousResults) {
        this(task, context, intent, previousResults, new ExecutionContext());
    }

    public ToolRequest {
        previousResults = previousResults == null ? List.of() : List.copyOf(previousResults);
    }
}
