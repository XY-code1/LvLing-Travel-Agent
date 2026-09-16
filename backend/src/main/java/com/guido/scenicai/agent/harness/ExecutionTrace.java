package com.guido.scenicai.agent.harness;

import java.time.Instant;

public record ExecutionTrace(
        int taskId,
        String toolName,
        ToolResult.Status status,
        Instant startTime,
        Instant endTime,
        long durationMs) {
}
