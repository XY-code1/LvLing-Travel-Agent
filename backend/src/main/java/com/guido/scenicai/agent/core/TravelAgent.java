package com.guido.scenicai.agent.core;

import com.guido.scenicai.agent.api.TravelAgentRequest;
import com.guido.scenicai.agent.api.TravelAgentResponse;
import com.guido.scenicai.agent.context.TravelContext;
import com.guido.scenicai.agent.context.TravelContextBuilder;
import com.guido.scenicai.agent.intent.IntentExtractor;
import com.guido.scenicai.agent.intent.TravelIntent;
import com.guido.scenicai.agent.harness.HarnessContext;
import com.guido.scenicai.agent.harness.HarnessEngine;
import com.guido.scenicai.agent.harness.HarnessResult;
import com.guido.scenicai.agent.planner.TaskPlanner;
import com.guido.scenicai.agent.planner.TravelTask;
import com.guido.scenicai.domain.trip.TravelPlan;
import com.guido.scenicai.integration.llm.LlmChatRequest;
import com.guido.scenicai.integration.llm.LlmClientRouter;
import com.guido.scenicai.integration.llm.LlmRuntimeStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.math.BigDecimal;
import com.guido.scenicai.tool.poi.PoiTool;
import com.guido.scenicai.integration.amap.AmapPoiProvider;

@Slf4j
@Service
public class TravelAgent {
    private final TravelContextBuilder contextBuilder;
    private final IntentExtractor intentExtractor;
    private final TaskPlanner taskPlanner;
    private final HarnessEngine harnessEngine;
    private final LlmClientRouter llmClientRouter;
    private final LlmRuntimeStatus llmRuntimeStatus;

    public TravelAgent(TravelContextBuilder contextBuilder, IntentExtractor intentExtractor,
                       TaskPlanner taskPlanner, HarnessEngine harnessEngine) {
        this(contextBuilder, intentExtractor, taskPlanner, harnessEngine, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TravelAgent(TravelContextBuilder contextBuilder, IntentExtractor intentExtractor,
                       TaskPlanner taskPlanner, HarnessEngine harnessEngine,
                       LlmClientRouter llmClientRouter, LlmRuntimeStatus llmRuntimeStatus) {
        this.contextBuilder = contextBuilder;
        this.intentExtractor = intentExtractor;
        this.taskPlanner = taskPlanner;
        this.harnessEngine = harnessEngine;
        this.llmClientRouter = llmClientRouter;
        this.llmRuntimeStatus = llmRuntimeStatus;
    }

    public TravelAgentResponse plan(TravelAgentRequest request) {
        long start = System.nanoTime();
        String requestId = UUID.randomUUID().toString();
        TravelContext initialContext = contextBuilder.build(request);
        TravelIntent intent = intentExtractor.extract(request, initialContext);
        TravelContext context = contextBuilder.enrich(initialContext, intent);
        List<TravelTask> tasks = taskPlanner.plan(intent, context);
        String destination = context.city() == null ? "待确认城市" : context.city();
        TravelPlan draft = new TravelPlan("DRAFT", destination + "旅行任务计划已生成，等待工具执行。", tasks);
        HarnessResult harnessResult = harnessEngine.execute(draft, new HarnessContext(context, intent));
        TravelPlan plan = new TravelPlan(harnessResult.planStatus(), draft.summary(), tasks);
        boolean llmCalled = false;
        boolean fallback = false;
        String answer;
        try {
            if (llmClientRouter == null) throw new IllegalStateException("LLM router unavailable");
            answer = llmClientRouter.chat(toLlmRequest(request, context, intent, harnessResult)).getContent();
            llmCalled = answer != null && !answer.isBlank();
            if (!llmCalled) throw new IllegalStateException("LLM returned empty content");
        } catch (Exception exception) {
            fallback = true;
            answer = "[Demo fallback] AI 服务暂时不可用，旅行工具执行结果已保留，请稍后重试。";
            log.warn("[TravelAgent] LLM call failed; using marked demo fallback: {}", exception.getClass().getSimpleName());
        }
        long latencyMs = Math.max(1, (System.nanoTime() - start) / 1_000_000L);
        List<TravelAgentResponse.ItineraryDay> itinerary = buildItinerary(harnessResult, intent);
        BigDecimal estimate = budgetEstimate(harnessResult);
        BigDecimal limit = intent.budget();
        TravelAgentResponse response = new TravelAgentResponse(context, intent, tasks, plan,
                harnessResult.executionResults(), harnessResult.executionTrace(), answer, latencyMs, llmCalled, fallback,
                itinerary, new TravelAgentResponse.BudgetSummary(estimate, limit == null || estimate == null ? null : limit.subtract(estimate)));
        String toolsCalled = tasks.stream().map(task -> task.type().name())
                .distinct().collect(Collectors.joining(","));
        LlmRuntimeStatus.Status status = llmRuntimeStatus == null
                ? new LlmRuntimeStatus.Status(false, "OPENAI_COMPATIBLE", null, false, false)
                : llmRuntimeStatus.current();
        log.info("[TravelAgent] requestId={} message={} intent={} destination={} days={} budget={} constraints={} llmCalled={} toolsCalled={} planGenerated={} fallback={} latencyMs={}",
                requestId, request.getMessage(), intent, intent.city(), intent.durationDays(), intent.budget(), intent.constraints(), llmCalled, toolsCalled, !itinerary.isEmpty(), fallback, latencyMs);
        return response;
    }

    private List<TravelAgentResponse.ItineraryDay> buildItinerary(HarnessResult result, TravelIntent intent) {
        List<AmapPoiProvider.Poi> pois = new ArrayList<>();
        result.executionResults().stream().filter(r -> "amap.poi".equals(r.toolName()) && r.data() instanceof List<?>).forEach(r -> {
            for (Object selection : (List<?>) r.data()) {
                if (selection instanceof PoiTool.PoiSelection value && value.candidates() != null) {
                    value.candidates().stream().limit(1).forEach(pois::add);
                }
            }
        });
        if (pois.isEmpty()) return List.of();
        int days = Math.max(1, intent.durationDays() == null ? 1 : intent.durationDays());
        List<TravelAgentResponse.ItineraryDay> out = new ArrayList<>();
        for (int day = 1; day <= days; day++) {
            List<TravelAgentResponse.ItineraryItem> items = new ArrayList<>();
            int offset = (day - 1) * 2;
            for (int i = 0; i < 2 && offset + i < pois.size(); i++) {
                AmapPoiProvider.Poi poi = pois.get(offset + i);
                if (i == 1) items.add(new TravelAgentResponse.ItineraryItem("12:00", "午餐与休息", "rest", 90,
                        "预留午休，控制全天强度", null, null, offset + i));
                items.add(new TravelAgentResponse.ItineraryItem(i == 0 ? "09:00" : "14:00", poi.name(), "poi", 120,
                        intent.constraints().isEmpty() ? "按景点顺序安排，保留休息时间" : "结合低强度约束，减少连续步行",
                        poi.coordinates().longitude(), poi.coordinates().latitude(), offset + i + 1));
            }
            items.add(new TravelAgentResponse.ItineraryItem("18:00", "晚餐与自由活动", "rest", 90,
                    "结束当天游览，避免晚间继续赶行程", null, null, offset + 3));
            if (!items.isEmpty()) out.add(new TravelAgentResponse.ItineraryDay(day, "轻松游览", items));
        }
        return List.copyOf(out);
    }

    private BigDecimal budgetEstimate(HarnessResult result) {
        return result.executionResults().stream().filter(r -> "budget.rule".equals(r.toolName())).map(r -> r.data())
                .filter(v -> v instanceof com.guido.scenicai.tool.budget.BudgetTool.BudgetEstimate)
                .map(v -> ((com.guido.scenicai.tool.budget.BudgetTool.BudgetEstimate) v).knownEstimate()).findFirst().orElse(null);
    }

    private LlmChatRequest toLlmRequest(TravelAgentRequest request, TravelContext context,
                                        TravelIntent intent, HarnessResult result) {
        LlmChatRequest llmRequest = new LlmChatRequest();
        llmRequest.setSystemPrompt("你是旅灵旅行助手。请自然、简洁地用中文回答；尊重同行人、行动能力、预算和必去地点。工具结果是事实来源，不要编造工具调用，只输出给用户的回答。");
        llmRequest.setUserMessage("用户消息：" + request.getMessage() + "\n提取意图：" + intent
                + "\n旅行上下文：" + context + "\n工具执行结果：" + result.executionResults());
        llmRequest.setTemperature(0.3);
        llmRequest.setMaxTokens(600);
        return llmRequest;
    }
}
