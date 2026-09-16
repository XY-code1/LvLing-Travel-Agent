package com.guido.scenicai.domain.trip;

import com.guido.scenicai.agent.planner.TravelTask;

import java.util.List;

public record TravelPlan(String status, String summary, List<TravelTask> tasks) {
}
