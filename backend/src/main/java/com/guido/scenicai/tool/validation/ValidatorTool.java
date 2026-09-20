package com.guido.scenicai.tool.validation;

import com.guido.scenicai.agent.harness.AgentTool;
import com.guido.scenicai.agent.harness.ToolRequest;
import com.guido.scenicai.agent.harness.ToolResult;
import com.guido.scenicai.agent.harness.ValidationResult;
import com.guido.scenicai.agent.planner.TravelTask;
import org.springframework.stereotype.Component;

@Component
public class ValidatorTool implements AgentTool {
    @Override public String toolName() { return "plan.validator"; }
    @Override public TravelTask.Type supportedTaskType() { return TravelTask.Type.VALIDATE_PLAN; }

    @Override
    public ToolResult execute(ToolRequest request) {
        ValidationResult validation = ValidationResult.validate(request.intent(), request.previousResults());
        if ("FAIL".equals(validation.validationStatus()))
            return ToolResult.failed(request, toolName(), validation, "PLAN_VALIDATION_FAILED",
                    String.join("；", validation.issues()));
        if (!validation.warnings().isEmpty())
            return ToolResult.degraded(request, toolName(), validation, "PLAN_VALIDATED_WITH_WARNINGS",
                    "行程已通过基础验证，但存在需要人工确认的事项");
        return ToolResult.success(request, toolName(), validation);
    }
}
