package com.guido.scenicai.tool.opening;

import com.guido.scenicai.agent.harness.AgentTool;
import com.guido.scenicai.agent.harness.ToolRequest;
import com.guido.scenicai.agent.harness.ToolResult;
import com.guido.scenicai.agent.planner.TravelTask;
import com.guido.scenicai.tool.poi.PoiTool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OpeningHoursTool implements AgentTool {
    @Override
    public String toolName() { return "poi.opening-hours"; }

    @Override
    public TravelTask.Type supportedTaskType() { return TravelTask.Type.CHECK_POI_OPENING; }

    @Override
    public ToolResult execute(ToolRequest request) {
        List<OpeningStatus> statuses = request.previousResults().stream()
                .filter(result -> "amap.poi".equals(result.toolName()) && result.data() instanceof List<?>)
                .flatMap(result -> ((List<?>) result.data()).stream())
                .filter(PoiTool.PoiSelection.class::isInstance)
                .map(PoiTool.PoiSelection.class::cast)
                .map(selection -> new OpeningStatus(selection.query(), "UNKNOWN", null,
                        "当前高德 POI 响应不含可验证的开放时间"))
                .toList();
        if (statuses.isEmpty()) {
            return ToolResult.failed(request, toolName(), "POI_RESULT_REQUIRED", "没有可检查开放时间的 POI");
        }
        return ToolResult.degraded(request, toolName(), statuses, "OPENING_HOURS_NOT_AVAILABLE",
                "开放时间数据源尚不可用，未假定景点处于开放状态");
    }

    public record OpeningStatus(String poi, String status, String hours, String evidence) {}
}
