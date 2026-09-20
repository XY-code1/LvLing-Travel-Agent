package com.guido.scenicai.agent.harness;

import com.guido.scenicai.agent.intent.TravelIntent;
import com.guido.scenicai.tool.budget.BudgetTool;
import com.guido.scenicai.tool.map.AMapRouteTool;
import com.guido.scenicai.tool.poi.PoiTool;
import com.guido.scenicai.module.city.vo.CityContextVO;

import java.util.ArrayList;
import java.util.List;

public record ValidationResult(String validationStatus, List<String> issues,
                               List<String> warnings, List<String> evidence) {
    public ValidationResult {
        issues = issues == null ? List.of() : List.copyOf(issues);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public boolean valid() { return "PASS".equals(validationStatus); }

    public static ValidationResult validate(TravelIntent intent, List<ToolResult> results) {
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> evidence = new ArrayList<>();
        ToolResult city = find(results, "city.context");
        if (!success(city)) issues.add("目的地城市上下文未成功加载");
        else if (city.data() instanceof CityContextVO context && context.getCity() != null) {
            if (!sameCity(intent.city(), context.getCity().getCityName()))
                issues.add("目的地不一致：Intent=" + intent.city() + "，CityContext=" + context.getCity().getCityName());
            else evidence.add("目的地城市上下文已由 City Discovery 验证：" + context.getCity().getCityName());
        }
        ToolResult poi = find(results, "amap.poi");
        if (!success(poi)) issues.add("必去 POI 查询未成功");
        else if (poi.data() instanceof List<?> selections) {
            for (String required : intent.requestedPois()) {
                boolean found = selections.stream().filter(PoiTool.PoiSelection.class::isInstance)
                        .map(PoiTool.PoiSelection.class::cast)
                        .anyMatch(item -> required.equals(item.query()) && !item.candidates().isEmpty());
                if (!found) issues.add("未找到必去景点：" + required);
                else evidence.add("已通过高德 POI 找到：" + required);
            }
        }
        ToolResult route = find(results, "amap.route");
        if (!success(route) || !(route.data() instanceof AMapRouteTool.RouteOutput output)) issues.add("真实路线不可用");
        else {
            evidence.add("高德路线可达，距离 " + output.distanceMeters() + " 米，预计 "
                    + Math.max(1, output.durationSeconds() / 60) + " 分钟");
            if (intent.mobilityConstraint() && output.distanceMeters() > 3000)
                warnings.add("低行动能力约束下步行路线过长（" + output.distanceMeters() + " 米），建议接驳或驾车");
            if (output.durationSeconds() > 12 * 60 * 60) issues.add("路线交通时间超过单日可执行范围");
            else evidence.add("路线交通时间未发现明显单日时间冲突");
        }
        ToolResult opening = find(results, "poi.opening-hours");
        if (opening == null || opening.status() == ToolResult.Status.FAILED) issues.add("开放状态未检查");
        else if (opening.status() == ToolResult.Status.DEGRADED)
            warnings.add("开放时间无法由当前数据源验证，出行前需二次确认");
        ToolResult budget = find(results, "budget.rule");
        if (budget == null || budget.status() == ToolResult.Status.FAILED) issues.add("预算检查未完成");
        else if (budget.data() instanceof BudgetTool.BudgetEstimate estimate) {
            evidence.add("已知费用估算 " + estimate.knownEstimate() + " 元，预算上限 " + estimate.limit() + " 元");
            if (!estimate.knownItemsWithinBudget()) issues.add("已知费用超过预算上限");
            if (estimate.tickets() == null) warnings.add("门票价格缺失，预算结论仅覆盖餐饮、交通和机动费用");
        }
        results.stream().filter(item -> item.status() == ToolResult.Status.FAILED)
                .forEach(item -> issues.add(item.toolName() + " 执行失败：" + item.message()));
        results.stream().filter(item -> item.status() == ToolResult.Status.SKIPPED)
                .forEach(item -> issues.add("必要 Tool 未注册：" + item.errorCode()));
        results.stream().filter(item -> item.status() == ToolResult.Status.DEGRADED
                        && !"poi.opening-hours".equals(item.toolName()) && !"budget.rule".equals(item.toolName()))
                .forEach(item -> warnings.add(item.toolName() + " 降级：" + item.message()));
        String status = issues.isEmpty() ? (warnings.isEmpty() ? "PASS" : "PASS_WITH_WARNINGS") : "FAIL";
        return new ValidationResult(status, issues.stream().distinct().toList(),
                warnings.stream().distinct().toList(), evidence.stream().distinct().toList());
    }

    public static ValidationResult from(HarnessResult result) {
        List<String> issues = result.executionResults().stream()
                .filter(item -> item.status() == ToolResult.Status.FAILED || item.status() == ToolResult.Status.SKIPPED)
                .map(item -> item.toolName() + ": " + item.message()).toList();
        List<String> warnings = result.executionResults().stream()
                .filter(item -> item.status() == ToolResult.Status.DEGRADED)
                .map(item -> item.toolName() + ": " + item.message()).toList();
        return new ValidationResult(issues.isEmpty() ? (warnings.isEmpty() ? "PASS" : "PASS_WITH_WARNINGS") : "FAIL",
                issues, warnings, List.of());
    }

    private static ToolResult find(List<ToolResult> results, String tool) {
        return results.stream().filter(item -> tool.equals(item.toolName())).findFirst().orElse(null);
    }
    private static boolean success(ToolResult result) {
        return result != null && result.status() == ToolResult.Status.SUCCESS;
    }
    private static boolean sameCity(String expected, String actual) {
        if (expected == null || actual == null) return false;
        return expected.replaceFirst("市$", "").equals(actual.replaceFirst("市$", ""));
    }
}
