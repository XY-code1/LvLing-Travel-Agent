package com.guido.scenicai.agent.api;

import com.guido.scenicai.agent.core.TravelAgent;
import com.guido.scenicai.agent.skill.TravelPlanningRequest;
import com.guido.scenicai.agent.skill.TravelPlanningResult;
import com.guido.scenicai.agent.skill.TravelPlanningSkill;
import com.guido.scenicai.common.result.Result;
import com.guido.scenicai.integration.llm.LlmRuntimeStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tourist/agent")
@RequiredArgsConstructor
public class TravelAgentController {
    private final TravelAgent travelAgent;
    private final TravelPlanningSkill travelPlanningSkill;
    private final LlmRuntimeStatus llmRuntimeStatus;

    @org.springframework.web.bind.annotation.GetMapping("/status")
    public Result<LlmRuntimeStatus.Status> status() {
        return Result.ok(llmRuntimeStatus.current());
    }

    @PostMapping("/plan")
    public Result<TravelAgentResponse> plan(@Valid @RequestBody TravelAgentRequest request) {
        return Result.ok(travelAgent.plan(request));
    }

    @PostMapping("/travel-planning")
    public Result<TravelPlanningResult> travelPlanning(@RequestBody TravelPlanningRequest request) {
        return Result.ok(travelPlanningSkill.plan(request));
    }
}
