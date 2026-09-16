package com.guido.scenicai.agent.planner;

import com.guido.scenicai.agent.context.TravelContext;
import com.guido.scenicai.agent.intent.TravelIntent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TaskPlanner {
    public List<TravelTask> plan(TravelIntent intent, TravelContext context) {
        List<TravelTask> tasks = new ArrayList<>();
        add(tasks, TravelTask.Type.RESOLVE_CITY, "确认旅行城市与城市上下文", true);
        if (context.date() != null) add(tasks, TravelTask.Type.CHECK_WEATHER, "检查旅行日期天气", false);
        add(tasks, TravelTask.Type.SEARCH_POI, "查询符合偏好与必去要求的景点", true);
        if (!intent.requestedPois().isEmpty()) {
            add(tasks, TravelTask.Type.CHECK_POI_OPENING, "检查必去景点开放信息", true);
        }
        add(tasks, TravelTask.Type.PLAN_ROUTE, "规划景点顺序与交通路线", true);
        if (context.budget() != null) add(tasks, TravelTask.Type.CHECK_BUDGET, "核算行程预算", true);
        add(tasks, TravelTask.Type.VALIDATE_PLAN, "检查计划约束与完整性", true);
        return List.copyOf(tasks);
    }

    private void add(List<TravelTask> tasks, TravelTask.Type type, String description, boolean required) {
        tasks.add(new TravelTask(tasks.size() + 1, type, description, required));
    }
}
