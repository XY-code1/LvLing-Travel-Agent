package com.guido.scenicai.tool.budget;

import com.guido.scenicai.agent.harness.AgentTool;
import com.guido.scenicai.agent.harness.ToolRequest;
import com.guido.scenicai.agent.harness.ToolResult;
import com.guido.scenicai.agent.planner.TravelTask;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class BudgetTool implements AgentTool {
    private static final BigDecimal MEAL_PER_PERSON_DAY = new BigDecimal("120");
    private static final BigDecimal LOCAL_TRANSPORT_ALLOWANCE = new BigDecimal("200");
    private static final BigDecimal CONTINGENCY = new BigDecimal("100");

    @Override
    public String toolName() { return "budget.rule"; }

    @Override
    public TravelTask.Type supportedTaskType() { return TravelTask.Type.CHECK_BUDGET; }

    @Override
    public ToolResult execute(ToolRequest request) {
        BigDecimal limit = request.context().budget();
        if (limit == null) return ToolResult.failed(request, toolName(), "BUDGET_REQUIRED", "未提供预算上限");
        int travelers = travelerCount(request.intent().travelers(), request.intent().partyType());
        int days = request.context().durationDays() == null ? 1 : request.context().durationDays();
        BigDecimal meals = MEAL_PER_PERSON_DAY.multiply(BigDecimal.valueOf(travelers * (long) days));
        BigDecimal knownEstimate = meals.add(LOCAL_TRANSPORT_ALLOWANCE).add(CONTINGENCY);
        BudgetEstimate estimate = new BudgetEstimate(limit, knownEstimate, meals, LOCAL_TRANSPORT_ALLOWANCE,
                CONTINGENCY, null, knownEstimate.compareTo(limit) <= 0,
                List.of("餐饮按每人每天120元规则估算", "市内交通预留200元", "机动费用预留100元", "门票价格尚未接入，未计入总额"));
        return ToolResult.degraded(request, toolName(), estimate, "TICKET_PRICE_NOT_AVAILABLE",
                "已完成可验证项预算估算；因门票价格缺失，不能断言最终总预算通过");
    }

    private int travelerCount(String travelers, String partyType) {
        if (travelers != null) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)").matcher(travelers);
            if (matcher.find()) return Math.max(1, Integer.parseInt(matcher.group(1)));
            if (travelers.contains("父母")) return 3;
        }
        return "FAMILY".equals(partyType) ? 3 : 1;
    }

    public record BudgetEstimate(BigDecimal limit, BigDecimal knownEstimate, BigDecimal meals,
                                 BigDecimal localTransport, BigDecimal contingency, BigDecimal tickets,
                                 boolean knownItemsWithinBudget, List<String> evidence) {}
}
