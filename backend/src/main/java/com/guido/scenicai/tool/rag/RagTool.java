package com.guido.scenicai.tool.rag;

import com.guido.scenicai.agent.harness.AgentTool;
import com.guido.scenicai.agent.harness.ToolRequest;
import com.guido.scenicai.agent.harness.ToolResult;
import com.guido.scenicai.agent.planner.TravelTask;
import com.guido.scenicai.module.knowledge.service.LocalRagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RagTool implements AgentTool {
    private final LocalRagService ragService;

    @Override
    public String toolName() { return "rag.local"; }

    @Override
    public TravelTask.Type supportedTaskType() { return TravelTask.Type.QUERY_RAG; }

    @Override
    public ToolResult execute(ToolRequest request) {
        String query = request.context().city() + " " + String.join(" ", request.intent().requestedPois())
                + " 旅行注意事项";
        LocalRagService.RagResult result = ragService.retrieve(null, query);
        if (!result.hit()) {
            return ToolResult.degraded(request, toolName(), result, "RAG_NO_RELEVANT_KNOWLEDGE",
                    "本地知识库没有命中与目的地相关的可靠资料");
        }
        return ToolResult.success(request, toolName(), result);
    }
}
