package com.guido.scenicai.agent.harness;

import com.guido.scenicai.agent.planner.TravelTask;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ToolRegistry {
    private final Map<TravelTask.Type, AgentTool> tools = new EnumMap<>(TravelTask.Type.class);

    public ToolRegistry(List<AgentTool> discoveredTools) {
        for (AgentTool tool : discoveredTools) {
            if (tools.putIfAbsent(tool.supportedTaskType(), tool) != null) {
                throw new IllegalStateException("Duplicate tool for task type " + tool.supportedTaskType());
            }
        }
    }

    public Optional<AgentTool> find(TravelTask.Type taskType) {
        return Optional.ofNullable(tools.get(taskType));
    }
}
