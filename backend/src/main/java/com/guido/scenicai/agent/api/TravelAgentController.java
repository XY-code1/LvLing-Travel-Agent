package com.guido.scenicai.agent.api;

import com.guido.scenicai.agent.core.TravelAgent;
import com.guido.scenicai.common.result.Result;
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

    @PostMapping("/plan")
    public Result<TravelAgentResponse> plan(@Valid @RequestBody TravelAgentRequest request) {
        return Result.ok(travelAgent.plan(request));
    }
}
