package com.guido.scenicai.agent.core;

import com.guido.scenicai.agent.api.TravelAgentRequest;
import com.guido.scenicai.agent.api.TravelAgentResponse;
import com.guido.scenicai.agent.context.TravelContext;
import com.guido.scenicai.agent.context.TravelContextBuilder;
import com.guido.scenicai.agent.intent.IntentExtractor;
import com.guido.scenicai.agent.intent.TravelIntent;
import com.guido.scenicai.agent.planner.TaskPlanner;
import com.guido.scenicai.agent.planner.TravelTask;
import com.guido.scenicai.domain.trip.TravelPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TravelAgent {
    private final TravelContextBuilder contextBuilder;
    private final IntentExtractor intentExtractor;
    private final TaskPlanner taskPlanner;

    public TravelAgentResponse plan(TravelAgentRequest request) {
        TravelContext initialContext = contextBuilder.build(request);
        TravelIntent intent = intentExtractor.extract(request, initialContext);
        TravelContext context = contextBuilder.enrich(initialContext, intent);
        List<TravelTask> tasks = taskPlanner.plan(intent, context);
        String destination = context.city() == null ? "待确认城市" : context.city();
        TravelPlan plan = new TravelPlan("DRAFT", destination + "旅行任务计划已生成，等待工具执行。", tasks);
        return new TravelAgentResponse(context, intent, tasks, plan);
    }
}
