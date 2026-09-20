package com.guido.scenicai.agent.harness;

public record ToolResult(
        int taskId,
        String toolName,
        Status status,
        Object data,
        String errorCode,
        String message) {

    public enum Status { SUCCESS, DEGRADED, FAILED, SKIPPED }

    public static ToolResult success(ToolRequest request, String toolName, Object data) {
        return new ToolResult(request.task().order(), toolName, Status.SUCCESS, data, null, null);
    }

    public static ToolResult failed(ToolRequest request, String toolName, String errorCode, String message) {
        return new ToolResult(request.task().order(), toolName, Status.FAILED, null, errorCode, message);
    }

    public static ToolResult failed(ToolRequest request, String toolName, Object data, String errorCode, String message) {
        return new ToolResult(request.task().order(), toolName, Status.FAILED, data, errorCode, message);
    }

    public static ToolResult degraded(ToolRequest request, String toolName, Object data, String errorCode, String message) {
        return new ToolResult(request.task().order(), toolName, Status.DEGRADED, data, errorCode, message);
    }

    public static ToolResult skipped(int taskId, String errorCode, String message) {
        return new ToolResult(taskId, null, Status.SKIPPED, null, errorCode, message);
    }
}
