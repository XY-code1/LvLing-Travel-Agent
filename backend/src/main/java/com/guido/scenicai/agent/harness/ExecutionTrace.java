package com.guido.scenicai.agent.harness;

import java.time.Instant;

public record ExecutionTrace(
        int taskId,
        String step,
        String toolName,
        ToolResult.Status status,
        Instant startTime,
        Instant endTime,
        long durationMs,
        String message,
        String inputSummary,
        String outputSummary,
        String source,
        String error) {

    public ExecutionTrace(int taskId, String step, String toolName, ToolResult.Status status,
                          Instant startTime, Instant endTime, long durationMs, String message) {
        this(taskId, step, toolName, status, startTime, endTime, durationMs, message,
                null, null, null, null);
    }
}
