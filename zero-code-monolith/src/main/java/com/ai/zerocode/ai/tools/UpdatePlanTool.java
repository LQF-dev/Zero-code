package com.ai.zerocode.ai.tools;

import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 计划管理工具。
 * <p>
 * AI 在生成或修改项目时，通过此工具创建和更新结构化的任务计划。
 * 每次调用都会完整替换当前计划（全量更新），并重置 Nag 计数器。
 * <p>
 * 前端通过 SSE 流中的 ToolRequestMessage（name="updatePlan"）识别计划事件，
 * 渲染为可视化的进度面板。
 */
@Slf4j
@Component
public class UpdatePlanTool extends BaseTool {

    @Resource
    private PlanTracker planTracker;

    @Tool("创建或更新任务计划。在开始生成项目或执行修改前，先用此工具列出计划；每完成一个阶段后更新进度。")
    public String updatePlan(
            @P("计划条目的 JSON 数组，每个条目包含 id（编号）、text（任务描述）、status（pending/in_progress/completed）")
            List<PlanItem> items,
            @ToolMemoryId Long appId
    ) {
        List<PlanTracker.PlanItem> planItems = items.stream()
                .map(item -> new PlanTracker.PlanItem(item.id, item.text, item.status))
                .toList();
        return planTracker.updatePlan(appId, planItems);
    }

    @Override
    public String getToolName() {
        return "updatePlan";
    }

    @Override
    public String getDisplayName() {
        return "更新计划";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        return "[计划已更新]";
    }

    /**
     * LangChain4j 用于反序列化工具参数的内部记录类。
     */
    public record PlanItem(String id, String text, String status) {
    }
}
