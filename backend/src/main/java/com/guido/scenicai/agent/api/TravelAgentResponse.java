package com.guido.scenicai.agent.api;

import com.guido.scenicai.agent.context.TravelContext;
import com.guido.scenicai.agent.intent.TravelIntent;
import com.guido.scenicai.agent.planner.TravelTask;
import com.guido.scenicai.domain.trip.TravelPlan;

import java.util.List;

public record TravelAgentResponse(
        TravelContext context,
        TravelIntent intent,
        List<TravelTask> tasks,
        TravelPlan plan) {
}
