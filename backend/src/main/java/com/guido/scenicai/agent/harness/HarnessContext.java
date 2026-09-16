package com.guido.scenicai.agent.harness;

import com.guido.scenicai.agent.context.TravelContext;
import com.guido.scenicai.agent.intent.TravelIntent;

public record HarnessContext(TravelContext travelContext, TravelIntent intent) {
}
