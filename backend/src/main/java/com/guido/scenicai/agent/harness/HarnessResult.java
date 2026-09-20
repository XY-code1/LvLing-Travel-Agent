package com.guido.scenicai.agent.harness;

import java.util.List;

public record HarnessResult(List<ToolResult> executionResults, List<ExecutionTrace> executionTrace) {
    public String planStatus() {
        boolean failed = executionResults.stream().anyMatch(result -> result.status() == ToolResult.Status.FAILED);
        boolean gaps = executionResults.stream().anyMatch(result -> result.status() == ToolResult.Status.SKIPPED
                || result.status() == ToolResult.Status.DEGRADED);
        return failed ? "EXECUTED_WITH_ERRORS" : gaps ? "EXECUTED_WITH_GAPS" : "EXECUTED";
    }
}
