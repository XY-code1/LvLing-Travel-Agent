package com.guido.scenicai.agent.harness;

import com.guido.scenicai.agent.context.TravelContext;
import com.guido.scenicai.agent.intent.TravelIntent;
import com.guido.scenicai.agent.planner.TravelTask;

public record ToolRequest(TravelTask task, TravelContext context, TravelIntent intent) {
}
