package com.guido.scenicai.agent.harness;

import com.guido.scenicai.agent.planner.TravelTask;

public interface AgentTool {
    String toolName();

    TravelTask.Type supportedTaskType();

    ToolResult execute(ToolRequest request);
}
